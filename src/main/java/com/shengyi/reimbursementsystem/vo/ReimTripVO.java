package com.shengyi.reimbursementsystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "补录行程返回视图对象")
public class ReimTripVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "报销单ID")
    private String reimId;

    @Schema(description = "出行人ID")
    private String travelerId;

    @Schema(description = "出行人工号")
    private String travelerNo;

    @Schema(description = "出行人姓名")
    private String travelerName;

    @Schema(description = "出发城市ID")
    private String departureCityId;

    @Schema(description = "出发城市名称")
    private String departureCityName;

    @Schema(description = "到达城市ID")
    private String arriveCityId;

    @Schema(description = "到达城市名称")
    private String arriveCityName;

    @Schema(description = "到达城市等级")
    private String arriveCityLevel;

    @Schema(description = "出发日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate departureDate;

    @Schema(description = "到达日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate arriveDate;

    @Schema(description = "行程天数")
    private Integer tripDays;

    @Schema(description = "行程说明")
    private String tripDesc;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creationTime;
}
