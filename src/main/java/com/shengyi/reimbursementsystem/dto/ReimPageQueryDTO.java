package com.shengyi.reimbursementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "报销单分页查询入参")
public class ReimPageQueryDTO {

    @NotNull(message = "当前页不能为空")
    @Schema(description = "当前页，默认1", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer current = 1;

    @NotNull(message = "每页大小不能为空")
    @Schema(description = "每页大小，默认10", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer size = 10;

    @Schema(description = "报销单号，模糊查询")
    private String reimNo;

    @Schema(description = "报销标题，模糊查询")
    private String reimTitle;

    @Schema(description = "出差事由，模糊查询")
    private String businessTripReason;

    @Schema(description = "费用归属公司ID")
    private String companyId;

    @Schema(description = "报销部门ID")
    private String departmentId;

    @Schema(description = "报销人ID")
    private String reimburserId;

    @Schema(description = "业务类型ID")
    private String businessTypeId;
}
