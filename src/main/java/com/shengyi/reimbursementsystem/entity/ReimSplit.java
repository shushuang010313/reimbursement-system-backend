package com.shengyi.reimbursementsystem.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shengyi.reimbursementsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fk_reim_split")
public class ReimSplit extends BaseEntity {

    private String reimId;
    private Integer sortNo;
    private String companyId;
    private String companyName;
    private String projectId;
    private String projectName;
    private BigDecimal splitRatio;
    private BigDecimal splitAmount;

    @TableLogic
    private Integer delFlag;
}
