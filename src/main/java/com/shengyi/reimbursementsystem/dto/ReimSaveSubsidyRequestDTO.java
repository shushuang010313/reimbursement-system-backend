package com.shengyi.reimbursementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "保存补助日历请求")
public class ReimSaveSubsidyRequestDTO {

    @NotBlank(message = "补助信息ID不能为空")
    @Schema(description = "补助信息ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subsidyId;

    @NotEmpty(message = "日历明细列表不能为空")
    @Schema(description = "日历明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ReimCalendarDTO> calendarList;
}
