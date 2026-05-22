package com.shengyi.reimbursementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "补助信息入参")
public class ReimSubsidyDTO {

    @Schema(description = "补助信息ID (更新时必传)")
    private String id;

    @Schema(description = "报销单ID")
    private String reimId;

    @Schema(description = "行程ID")
    private String tripId;

    @Schema(description = "出行人ID")
    private String travelerId;

    @Schema(description = "出行人姓名")
    private String travelerName;

    @Schema(description = "出差开始日期")
    private LocalDate tripStartDate;

    @Schema(description = "出差结束日期")
    private LocalDate tripEndDate;

    @Schema(description = "补助天数")
    private Integer subsidyDays;

    @Schema(description = "补助城市ID")
    private String subsidyCityId;

    @Schema(description = "补助城市名称")
    private String subsidyCityName;

    @Schema(description = "申请金额（标准金额合计）")
    private BigDecimal applyAmount;

    @Schema(description = "补助金额（实际补助金额合计）")
    private BigDecimal subsidyAmount;

    @Schema(description = "餐费补助金额")
    private BigDecimal mealSubsidy;

    @Schema(description = "交通补助金额")
    private BigDecimal transportSubsidy;

    @Schema(description = "通讯补助金额")
    private BigDecimal phoneSubsidy;
}
