package com.shengyi.reimbursementsystem.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengyi.reimbursementsystem.annotation.Idempotent;
import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 接口防抖与幂等性 AOP 切面拦截器
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 【答辩重点】为什么用 Lua 脚本保证绝对原子性？
    // 如果不用 Lua，就需要先 get 查是否存在，如果不存在再 setnx，然后再 expire。这三步操作在网络高并发下不是原子性的，会导致防抖失效。
    // 使用 Lua 脚本，把 setnx 和 expire 打包成一个命令发给 Redis，Redis 单线程执行，绝对保证原子性。这是答辩时的加分核心点！
    private static final String LUA_SCRIPT = 
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
            "    redis.call('expire', KEYS[1], ARGV[2]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 环绕通知：在目标方法执行前后进行拦截和处理。
     * 该方法是实现防抖和幂等的具体逻辑核心。
     *
     * @param joinPoint  连接点，代表被拦截的目标方法，可以通过它获取方法参数，并控制方法的执行（放行）
     * @param idempotent 方法上标注的 @Idempotent 注解实例，可以通过它获取注解上配置的超时时间和提示信息
     * @return 目标方法执行后的返回值
     * @throws Throwable 目标方法抛出的任何异常都会向上抛出
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 1. 获取当前 HTTP 请求上下文
        // RequestContextHolder 是 Spring 提供的用于获取当前线程绑定的 Request 属性的工具类
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 如果获取不到 Request（例如在非 Web 环境下的内部调用），则直接放行，不做拦截
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        
        // 2. 提取用户唯一标识，用于区分不同用户的请求
        // 提取用户信息（如有），为了简化演示，这里假设可以从 header 中获取 userId，若无则使用IP兜底
        String userId = request.getHeader("user-id");
        if (userId == null || userId.isEmpty()) {
            userId = request.getRemoteAddr();
        }

        // 3. 构建高精度的幂等 Key 组成部分
        // 获取请求的 URI，限定接口维度
        String uri = request.getRequestURI();
        // 获取目标方法的入参（数组）
        Object[] args = joinPoint.getArgs();
        String argsString = "";
        try {
            // 将所有入参序列化为 JSON 字符串，为下一步提取 MD5 做准备
            argsString = objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            log.error("幂等切面序列化参数失败，无法执行防抖逻辑", e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR.getCode(), "系统内部异常：参数序列化失败");
        }

        // 【答辩重点】如何精准识别“同一次重复请求”？
        // 答：防抖拦截不能误伤正常的不同请求。所以这里的 Key 是【请求URI】+【用户ID】+【入参的MD5】组合而成的。
        // 只有同一个用户，在短时间内，向同一个接口，提交了完全一模一样的参数（MD5一样），才会被拦截。
        // 3.1 对参数进行 MD5 摘要计算，避免参数字符串过长导致 Redis Key 臃肿
        String hash = DigestUtils.md5DigestAsHex(argsString.getBytes(StandardCharsets.UTF_8));
        // 3.2 最终拼装成 Redis 中的 Key 格式，例如 idempotent:/api/submit:1001:a1b2c3d4e5f6...
        String redisKey = "idempotent:" + uri + ":" + userId + ":" + hash;

        // 4. 获取注解上配置的防抖/幂等超时时间（过期时间）
        long timeout = idempotent.timeout();

        // 5. 准备并执行 Lua 脚本以保证原子性操作
        // 构造 Lua 脚本对象
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        // 执行 Lua 脚本 
        // KEYS[1] = redisKey: 刚才拼接好的唯一 Key
        // ARGV[1] = "1": 占位用的 Value（其实写什么都行，这里写 "1"）
        // ARGV[2] = timeout: Key 的过期时间，单位为秒
        Long result = stringRedisTemplate.execute(
                redisScript, 
                Collections.singletonList(redisKey), 
                "1", 
                String.valueOf(timeout)
        );

        // 6. 核心判断：根据 Lua 脚本执行结果决定是否拦截
        // Lua 脚本返回 1 代表 setnx 成功，也就是这是第一次请求
        // Lua 脚本返回 0 代表 setnx 失败，说明 Redis 中已经存在这个 Key，这是一次重复请求！
        if (result != null && result == 0L) {
            log.warn("【接口幂等性拦截】发现重复请求拦截，URI: {}, Key: {}", uri, redisKey);
            // 抛出业务异常，阻断代码继续执行，返回给前端注解上配置的提示信息（例如：请勿重复提交）
            throw new BusinessException(500, idempotent.message());
        }

        // 7. 放行
        // 如果是第一次请求（即没有抛出异常），调用 joinPoint.proceed() 真正去执行 Controller 里的业务方法
        return joinPoint.proceed();
    }
}
