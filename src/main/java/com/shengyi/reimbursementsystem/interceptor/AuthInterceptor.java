package com.shengyi.reimbursementsystem.interceptor;

import com.shengyi.reimbursementsystem.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 模拟从 Header 获取 Token 并解析出 userId
        // String token = request.getHeader("Authorization");
        // String userId = JwtUtils.parse(token).getUserId();
        
        // 为了目前项目测试方便，这里直接硬编码一个当前用户ID（后续接入真实登录可替换）
        String simulatedUserId = "U10001"; 
        
        // 如果 header 里面传了特定的用户标识也可以用来模拟
        String headerUserId = request.getHeader("user-id");
        if (headerUserId == null || headerUserId.isEmpty()) {
            headerUserId = request.getHeader("X-User-Id");
        }
        if (headerUserId != null && !headerUserId.isEmpty()) {
            simulatedUserId = headerUserId;
        }

        log.debug("AuthInterceptor: 提取到当前用户 userId = {}", simulatedUserId);
        UserContext.setUserId(simulatedUserId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清理 ThreadLocal 防止内存泄漏
        UserContext.clear();
    }
}
