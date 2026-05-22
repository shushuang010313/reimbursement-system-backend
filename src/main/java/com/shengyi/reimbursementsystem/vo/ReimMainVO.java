package com.shengyi.reimbursementsystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "报销单主单返回视图对象")
public class ReimMainVO {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "报销单号")
    private String reimNo;

    @Schema(description = "单据状态：0-草稿 1-已完成 2-已作废")
    private Integer reimStatus;

    @Schema(description = "报销标题")
    private String reimbursementTitle;

    @Schema(description = "出差事由")
    private String businessTripReason;

    @Schema(description = "报销人ID")
    private String reimburserId;

    @Schema(description = "报销人工号")
    private String reimburserNo;

    @Schema(description = "报销人姓名")
    private String reimburserName;

    @Schema(description = "报销部门ID")
    private String reimDepartmentId;

    @Schema(description = "报销部门编号")
    private String reimDepartmentNo;

    @Schema(description = "报销部门名称")
    private String reimDepartmentName;

    @Schema(description = "费用归属公司ID")
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

    @Schema(description = "创建人ID")
    private String createUserId;

    @Schema(description = "创建人工号")
    private String createUserNo;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creationTime;

    @Schema(description = "最后更新人ID")
    private String updateUserId;

    @Schema(description = "最后更新人姓名")
    private String updateUserName;

    @Schema(description = "最后更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
