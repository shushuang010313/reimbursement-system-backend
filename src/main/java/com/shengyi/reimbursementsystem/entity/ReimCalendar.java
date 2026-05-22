package com.shengyi.reimbursementsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengyi.reimbursementsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fk_reim_calendar")
public class ReimCalendar extends BaseEntity {

    private String reimId;
    private String subsidyId;
    private LocalDate tripDate;
    private String weekDay;
    private String subsidyCityId;
    private String subsidyCityName;
    private Integer mealChecked;
    private BigDecimal mealStandard;
    private BigDecimal mealAmount;
    private Integer transportChecked;
    private BigDecimal transportStandard;
    private BigDecimal transportAmount;
    private Integer phoneChecked;
    private BigDecimal phoneStandard;
    private BigDecimal phoneAmount;
}
