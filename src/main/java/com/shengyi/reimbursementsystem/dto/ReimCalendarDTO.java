package com.shengyi.reimbursementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "补助日历明细入参")
public class ReimCalendarDTO {

    @Schema(description = "日历记录ID")
    private String id;

    @Schema(description = "报销单ID")
    private String reimId;

    @Schema(description = "补助信息ID")
    private String subsidyId;

    @Schema(description = "出差日期")
    private LocalDate tripDate;

    @Schema(description = "星期（如：星期一）")
    private String weekDay;

    @Schema(description = "补助城市ID")
    private String subsidyCityId;

    @Schema(description = "补助城市名称")
    private String subsidyCityName;

    @Schema(description = "餐费是否勾选：0-否 1-是")
    private Integer mealChecked;

    @Schema(description = "餐费标准金额")
    private BigDecimal mealStandard;

    @Schema(description = "餐费补助金额")
    private BigDecimal mealAmount;

    @Schema(description = "交通是否勾选：0-否 1-是")
    private Integer transportChecked;

    @Schema(description = "交通标准金额")
    private BigDecimal transportStandard;

    @Schema(description = "交通补助金额")
    private BigDecimal transportAmount;

    @Schema(description = "通讯是否勾选：0-否 1-是")
    private Integer phoneChecked;

    @Schema(description = "通讯标准金额")
    private BigDecimal phoneStandard;

    @Schema(description = "通讯补助金额")
    private BigDecimal phoneAmount;
}
