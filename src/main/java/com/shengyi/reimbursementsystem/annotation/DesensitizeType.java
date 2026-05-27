package com.shengyi.reimbursementsystem.annotation;

/**
 * 敏感数据脱敏类型枚举
 */
public enum DesensitizeType {
    
    /**
     * 身份证：显示前6位和后4位，中间隐藏（如：110105********1234）
     */
    ID_CARD,
    
    /**
     * 银行卡：显示前4位和后4位，中间隐藏（如：6222****1234）
     */
    BANK_CARD,
    
    /**
     * 手机号：显示前3位和后4位，中间隐藏（如：138****1234）
     */
    PHONE,
    
    /**
     * 默认（全部变为星号或不处理，取决于具体实现，这里演示为全部星号）
     */
    DEFAULT
}
