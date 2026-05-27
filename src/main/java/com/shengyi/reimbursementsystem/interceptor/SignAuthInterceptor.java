package com.shengyi.reimbursementsystem.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Slf4j
public class SignAuthInterceptor implements HandlerInterceptor {

    private static final String SECRET_KEY = "FccReimBpmSecretKey@2025";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String timestamp = request.getHeader("X-Timestamp");
        String sign = request.getHeader("X-Sign");

        if (timestamp == null || sign == null) {
            log.warn("SignAuthInterceptor: 缺少签名参数");
            return errorResponse(response, "缺少签名参数");
        }

        // 简单的防重放校验（5分钟内有效）
        long reqTime;
        try {
            reqTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return errorResponse(response, "非法的时间戳格式");
        }
        if (Math.abs(System.currentTimeMillis() - reqTime) > 5 * 60 * 1000) {
            return errorResponse(response, "请求已过期");
        }

        // 验证签名: MD5(timestamp + secret)
        // 生产环境应该加上 payload 的摘要，为了演示这里使用 timestamp 加盐
        String expectedSign = DigestUtils.md5DigestAsHex((timestamp + SECRET_KEY).getBytes());
        if (!expectedSign.equalsIgnoreCase(sign)) {
            log.warn("SignAuthInterceptor: 签名校验失败, expected={}, actual={}", expectedSign, sign);
            return errorResponse(response, "签名校验失败");
        }

        return true;
    }

    private boolean errorResponse(HttpServletResponse response, String msg) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(ErrorCodeEnum.PARAM_ERROR.getCode(), msg);
        PrintWriter writer = response.getWriter();
        writer.print(objectMapper.writeValueAsString(result));
        writer.flush();
        writer.close();
        return false;
    }
}
