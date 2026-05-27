package com.shengyi.reimbursementsystem.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.shengyi.reimbursementsystem.config.JsonEncryptSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感数据加密与脱敏注解
 * <p>
 * 1. 作用于实体类字段时，AOP 拦截器（MyBatis 插件）将拦截入库和出库操作，实现数据库的透明加密（数据库存密文，内存中为明文）。
 * 2. 返回给前端时，Jackson 将自动调用 JsonEncryptSerializer 根据指定的类型进行脱敏处理。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonSerialize(using = JsonEncryptSerializer.class)
public @interface JsonEncrypt {

    /**
     * 脱敏类型，决定了如何对前端展示脱敏后的字符串
     */
    DesensitizeType value() default DesensitizeType.DEFAULT;

}
