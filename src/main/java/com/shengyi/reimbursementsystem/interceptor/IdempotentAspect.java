package com.shengyi.reimbursementsystem.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengyi.reimbursementsystem.annotation.Idempotent;
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

    // Lua 脚本保证绝对原子性
    private static final String LUA_SCRIPT = 
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
            "    redis.call('expire', KEYS[1], ARGV[2]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        
        // 提取用户信息（如有），为了简化演示，这里假设可以从header中获取 userId，若无则使用IP
        String userId = request.getHeader("user-id");
        if (userId == null || userId.isEmpty()) {
            userId = request.getRemoteAddr();
        }

        // 构建幂等 Key 的组成部分：请求URI + 用户ID + 入参的MD5散列
        String uri = request.getRequestURI();
        Object[] args = joinPoint.getArgs();
        String argsString = "";
        try {
            argsString = objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            log.warn("幂等切面序列化参数失败", e);
        }

        String hash = DigestUtils.md5DigestAsHex(argsString.getBytes(StandardCharsets.UTF_8));
        String redisKey = "idempotent:" + uri + ":" + userId + ":" + hash;

        long timeout = idempotent.timeout();

        // 构造 Lua 脚本对象
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        // 执行 Lua 脚本 (KEYS[1] = redisKey, ARGV[1] = "1", ARGV[2] = timeout)
        Long result = stringRedisTemplate.execute(
                redisScript, 
                Collections.singletonList(redisKey), 
                "1", 
                String.valueOf(timeout)
        );

        if (result != null && result == 0L) {
            log.warn("【接口幂等性拦截】发现重复请求拦截，URI: {}, Key: {}", uri, redisKey);
            throw new BusinessException(500, idempotent.message());
        }

        // 放行
        return joinPoint.proceed();
    }
}
