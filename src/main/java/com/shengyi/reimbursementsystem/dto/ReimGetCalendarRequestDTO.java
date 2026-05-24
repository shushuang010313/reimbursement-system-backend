package com.shengyi.reimbursementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "获取补助日历请求")
public class ReimGetCalendarRequestDTO {

    @NotBlank(message = "补助信息ID不能为空")
    @Schema(description = "补助信息ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subsidyId;
}
