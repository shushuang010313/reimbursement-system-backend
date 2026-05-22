package com.shengyi.reimbursementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "补录行程入参")
public class ReimTripDTO {

    @Schema(description = "行程ID (更新时必传)")
    private String id;

    @NotBlank(message = "出行人ID不能为空")
    @Schema(description = "出行人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String travelerId;

    @Schema(description = "出行人工号")
    private String travelerNo;

    @NotBlank(message = "出行人姓名不能为空")
    @Schema(description = "出行人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String travelerName;

    @NotBlank(message = "出发城市ID不能为空")
    @Schema(description = "出发城市ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String departureCityId;

    @Schema(description = "出发城市名称")
    private String departureCityName;

    @NotBlank(message = "到达城市ID不能为空")
    @Schema(description = "到达城市ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String arriveCityId;

    @Schema(description = "到达城市名称")
    private String arriveCityName;

    @Schema(description = "到达城市等级：1-一线 2-二线 3-三线")
    private String arriveCityLevel;

    @NotNull(message = "出发日期不能为空")
    @Schema(description = "出发日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate departureDate;

    @NotNull(message = "到达日期不能为空")
    @Schema(description = "到达日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate arriveDate;

    @Schema(description = "行程天数（到达日期-出发日期+1）")
    private Integer tripDays;

    @Schema(description = "行程说明")
    private String tripDesc;
}
