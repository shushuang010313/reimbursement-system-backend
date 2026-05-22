package com.shengyi.reimbursementsystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "费用分摊返回视图对象")
public class ReimSplitVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "报销单ID")
    private String reimId;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "费用归属公司ID")
    private String companyId;

    @Schema(description = "费用归属公司名称")
    private String companyName;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "分摊比例")
    private BigDecimal splitRatio;

    @Schema(description = "分摊金额")
    private BigDecimal splitAmount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creationTime;
}
