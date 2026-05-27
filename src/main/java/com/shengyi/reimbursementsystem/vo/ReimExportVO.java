package com.shengyi.reimbursementsystem.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ReimExportVO {

    @ExcelProperty("报销单号")
    @ColumnWidth(20)
    private String reimNo;

    @ExcelProperty("报销标题")
    @ColumnWidth(25)
    private String reimbursementTitle;

    @ExcelProperty("报销人")
    @ColumnWidth(15)
    private String reimburserName;

    @ExcelProperty("报销部门")
    @ColumnWidth(20)
    private String reimDepartmentName;

    @ExcelProperty("出差事由")
    @ColumnWidth(30)
    private String businessTripReason;

    @ExcelProperty("补助总金额")
    @ColumnWidth(15)
    private BigDecimal subsidyTotal;

    @ExcelProperty("收款人身份证号(脱敏)")
    @ColumnWidth(25)
    private String payeeIdCard;

    @ExcelProperty("收款人银行账号(脱敏)")
    @ColumnWidth(25)
    private String payeeBankAccount;

    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    private Date creationTime;

    @ExcelProperty("单据状态")
    @ColumnWidth(15)
    private String statusDesc;
}
