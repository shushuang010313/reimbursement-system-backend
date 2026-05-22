package com.shengyi.reimbursementsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.shengyi.reimbursementsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fk_reim_subsidy")
public class ReimSubsidy extends BaseEntity {

    private String reimId;
    private String tripId;
    private String travelerId;
    private String travelerName;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;
    private Integer subsidyDays;
    private String subsidyCityId;
    private String subsidyCityName;
    private BigDecimal applyAmount;
    private BigDecimal subsidyAmount;
    private BigDecimal mealSubsidy;
    private BigDecimal transportSubsidy;
    private BigDecimal phoneSubsidy;

    @TableLogic
    private Integer delFlag;
}
