package com.shengyi.reimbursementsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    
    @Version
    private Integer version;
    
    private String createUserId;
    private String createUserNo;
    private String createUserName;
    
    private String updateUserId;
    private String updateUserName;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer delFlag;
}
