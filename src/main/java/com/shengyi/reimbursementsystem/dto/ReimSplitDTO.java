package com.shengyi.reimbursementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "费用分摊入参")
public class ReimSplitDTO {

    @Schema(description = "分摊记录ID (更新时必传)")
    private String id;

    @Schema(description = "报销单ID")
    private String reimId;

    @Schema(description = "排序号（第1行不可编辑）")
    private Integer sortNo;

    @Schema(description = "费用归属公司ID")
    private String companyId;

    @Schema(description = "费用归属公司名称")
    private String companyName;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "分摊比例（如0.3000表示30%）")
    private BigDecimal splitRatio;

    @Schema(description = "分摊金额")
    private BigDecimal splitAmount;
}
