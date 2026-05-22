package com.shengyi.reimbursementsystem.common;

/**
 * 统一错误码枚举
 */
public enum ErrorCodeEnum {
    
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(500, "系统内部异常"),
    PARAM_ERROR(400, "参数错误"),
    
    // 业务错误码
    REIM_001(404, "报销单不存在"),
    REIM_002(400, "报销单状态不允许该操作"),
    REIM_003(400, "行程时间重复"),
    REIM_004(400, "分摊比例超过100%"),
    REIM_005(400, "必填字段缺失");

    private final Integer code;
    private final String message;

    ErrorCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
