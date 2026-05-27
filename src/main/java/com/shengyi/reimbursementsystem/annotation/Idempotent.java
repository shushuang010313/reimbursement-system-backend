package com.shengyi.reimbursementsystem.annotation;

import java.lang.annotation.*;

/**
 * 接口防抖与幂等性注解
 * 配合 IdempotentAspect 拦截器，使用 Redis + Lua 脚本实现高并发防重发
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 防抖/幂等有效时间（单位：秒）
     * 默认 5 秒内同样的请求（相同参数）会被拦截
     */
    long timeout() default 5;

    /**
     * 触发防重发时的提示语
     */
    String message() default "操作太频繁，请勿重复点击";
}
