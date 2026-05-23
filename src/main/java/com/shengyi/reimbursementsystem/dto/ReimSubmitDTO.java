package com.shengyi.reimbursementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "报销单提交入参")
public class ReimSubmitDTO {

    @NotBlank(message = "报销单ID不能为空")
    @Schema(description = "报销单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @NotNull(message = "乐观锁版本号不能为空")
    @Schema(description = "乐观锁版本号，防止并发提交", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
}
