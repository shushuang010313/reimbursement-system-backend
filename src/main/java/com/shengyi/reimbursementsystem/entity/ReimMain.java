package com.shengyi.reimbursementsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.shengyi.reimbursementsystem.annotation.DesensitizeType;
import com.shengyi.reimbursementsystem.annotation.JsonEncrypt;
import com.shengyi.reimbursementsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fk_reim_main")
public class ReimMain extends BaseEntity {

    private String reimNo;
    private Integer reimStatus;
    private String reimbursementTitle;
    private String businessTripReason;
    
    private String reimburserId;
    private String reimburserNo;
    private String reimburserName;
    
    private String reimDepartmentId;
    private String reimDepartmentNo;
    private String reimDepartmentName;
    
    private String reimCompanyId;
    private String reimCompanyNo;
    private String reimCompanyName;
    
    private String businessTypeId;
    private String businessTypeNo;
    private String businessTypeName;
    
    private BigDecimal subsidyTotal;
    private BigDecimal mealAllowance;
    private BigDecimal transportationAllowance;
    private BigDecimal phoneAllowance;
    
    private String remarks;

    // 【学习指引】数据脱敏：此字段在返回给前端（被 Jackson 序列化）时，会通过自定义序列化器进行身份证号打码。
    @JsonEncrypt(DesensitizeType.ID_CARD)
    private String payeeIdCard;

    // 【学习指引】数据脱敏：同上，对银行卡号进行安全脱敏打码。
    @JsonEncrypt(DesensitizeType.BANK_CARD)
    private String payeeBankAccount;
    
    // 【学习指引】乐观锁标记：配合 OptimisticLockerInnerInterceptor，更新时自动带上版本控制，防止并发脏写。
    @Version
    private Integer version;
    
    @TableField(fill = FieldFill.INSERT)
    private String createUserId;
    private String createUserNo;
    private String createUserName;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateUserId;
    private String updateUserName;
    
    // 【学习指引】自动填充字段：通过 MetaObjectHandler，在发生 INSERT 或 UPDATE 操作时自动注入当前时间。
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 【学习指引】逻辑删除标记：调用 MyBatis-Plus 的查询或删除方法时，引擎会自动过滤 / 修改该字段（例如查询时自动追加 WHERE del_flag=0）。
    @TableLogic
    private Integer delFlag;
}
