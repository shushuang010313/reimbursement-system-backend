package com.shengyi.reimbursementsystem.config;

import com.shengyi.reimbursementsystem.interceptor.AuthInterceptor;
import com.shengyi.reimbursementsystem.interceptor.SignAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册全局鉴权拦截器
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/**")
                // 放行swagger和静态资源
                .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/fccapi/REIM_UpdateStatus");

        // 注册回调验签拦截器
        registry.addInterceptor(new SignAuthInterceptor())
                .addPathPatterns("/fccapi/REIM_UpdateStatus");
    }
}
