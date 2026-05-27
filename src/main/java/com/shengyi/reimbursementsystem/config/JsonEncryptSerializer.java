package com.shengyi.reimbursementsystem.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.shengyi.reimbursementsystem.annotation.DesensitizeType;
import com.shengyi.reimbursementsystem.annotation.JsonEncrypt;

import java.io.IOException;

/**
 * Jackson 自定义序列化器，用于在将对象转为 JSON 返回给前端时进行动态脱敏
 */
public class JsonEncryptSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType desensitizeType;

    public JsonEncryptSerializer() {
    }

    public JsonEncryptSerializer(DesensitizeType desensitizeType) {
        this.desensitizeType = desensitizeType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }

        String maskedValue = value;
        if (desensitizeType != null) {
            switch (desensitizeType) {
                case ID_CARD:
                    // 身份证：保留前6后4
                    if (value.length() == 18) {
                        maskedValue = value.substring(0, 6) + "********" + value.substring(14);
                    } else if (value.length() >= 10) {
                        maskedValue = value.substring(0, 4) + "****" + value.substring(value.length() - 4);
                    }
                    break;
                case BANK_CARD:
                    // 银行卡：保留前4后4
                    if (value.length() >= 8) {
                        maskedValue = value.substring(0, 4) + "****" + value.substring(value.length() - 4);
                    }
                    break;
                case PHONE:
                    // 手机号：保留前3后4
                    if (value.length() == 11) {
                        maskedValue = value.substring(0, 3) + "****" + value.substring(7);
                    }
                    break;
                case DEFAULT:
                default:
                    maskedValue = "******";
                    break;
            }
        }
        
        gen.writeString(maskedValue);
    }

    /**
     * 获取属性上的注解，并根据注解配置创建新的序列化器实例
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            JsonEncrypt jsonEncrypt = property.getAnnotation(JsonEncrypt.class);
            if (jsonEncrypt == null) {
                jsonEncrypt = property.getContextAnnotation(JsonEncrypt.class);
            }
            if (jsonEncrypt != null) {
                return new JsonEncryptSerializer(jsonEncrypt.value());
            }
        }
        return this;
    }
}
