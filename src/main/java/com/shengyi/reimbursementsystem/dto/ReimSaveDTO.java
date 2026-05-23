package com.shengyi.reimbursementsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "报销单主单保存入参")
public class ReimSaveDTO {

    @Schema(description = "报销单ID (更新时必传)")
    private String id;

    @NotBlank(message = "报销标题不能为空")
    @Schema(description = "报销标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reimbursementTitle;

    @NotBlank(message = "出差事由不能为空")
    @Schema(description = "出差事由", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessTripReason;

    @NotBlank(message = "报销人ID不能为空")
    @Schema(description = "报销人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reimburserId;

    @Schema(description = "报销人工号")
    private String reimburserNo;

    @NotBlank(message = "报销人姓名不能为空")
    @Schema(description = "报销人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reimburserName;

    @Schema(description = "报销部门ID")
    private String reimDepartmentId;

    @Schema(description = "报销部门编号")
    private String reimDepartmentNo;

    @Schema(description = "报销部门名称")
    private String reimDepartmentName;

    @NotNull(message = "费用归属公司不能为空")
    @Schema(description = "费用归属公司ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reimCompanyId;

    @Schema(description = "费用归属公司编号")
    private String reimCompanyNo;

    @Schema(description = "费用归属公司名称")
    private String reimCompanyName;

    @Schema(description = "业务类型ID")
    private String businessTypeId;

    @Schema(description = "业务类型编号")
    private String businessTypeNo;

    @Schema(description = "业务类型名称")
    private String businessTypeName;

    @Schema(description = "补助总金额")
    private BigDecimal subsidyTotal;

    @Schema(description = "餐费补助")
    private BigDecimal mealAllowance;

    @Schema(description = "交通补助")
    private BigDecimal transportationAllowance;

    @Schema(description = "通讯补助")
    private BigDecimal phoneAllowance;

    @Schema(description = "备注信息")
    private String remarks;

    @Schema(description = "补录行程列表")
    private List<ReimTripDTO> tripList;

    @Schema(description = "费用分摊列表")
    private List<ReimSplitDTO> splitList;
}
