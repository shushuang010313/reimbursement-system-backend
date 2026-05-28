package com.shengyi.reimbursementsystem.interceptor;

import com.shengyi.reimbursementsystem.annotation.JsonEncrypt;
import com.shengyi.reimbursementsystem.utils.AESUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.mapping.BoundSql;

/**
 * MyBatis 拦截器，用于透明实现数据库的敏感字段加解密
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class CryptoInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        Object[] args = invocation.getArgs();
        Object parameter = args[1];

        // 1. 如果是插入或更新操作，加密实体中的敏感字段
        if ("update".equals(methodName)) {
            encryptFields(parameter);
        }

        // 2. 执行真实的 SQL 操作
        Object result = invocation.proceed();

        // 3. 如果是查询操作，解密返回结果中的敏感字段
        if ("query".equals(methodName)) {
            if (result instanceof ArrayList) {
                List<?> list = (ArrayList<?>) result;
                for (Object item : list) {
                    decryptFields(item);
                }
            } else {
                decryptFields(result);
            }
        }
        
        // 4. 如果是插入或更新后，实体中的密文为了不影响后续内存操作，需要解密回明文
        if ("update".equals(methodName)) {
            decryptFields(parameter);
        }

        return result;
    }

    /**
     * 遍历对象属性，进行加密
     */
    private void encryptFields(Object parameter) throws IllegalAccessException {
        if (parameter == null) return;
        Field[] fields = parameter.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(JsonEncrypt.class)) {
                field.setAccessible(true);
                Object value = field.get(parameter);
                if (value instanceof String) {
                    String encrypted = AESUtils.encrypt((String) value);
                    field.set(parameter, encrypted);
                }
            }
        }
    }

    /**
     * 遍历对象属性，进行解密
     */
    private void decryptFields(Object result) throws IllegalAccessException {
        if (result == null) return;
        Field[] fields = result.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(JsonEncrypt.class)) {
                field.setAccessible(true);
                Object value = field.get(result);
                if (value instanceof String) {
                    String decrypted = AESUtils.decrypt((String) value);
                    field.set(result, decrypted);
                }
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
