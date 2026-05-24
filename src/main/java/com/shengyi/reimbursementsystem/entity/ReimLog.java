package com.shengyi.reimbursementsystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shengyi.reimbursementsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fk_reim_log")
public class ReimLog extends BaseEntity {
    private String reimId;
    private String action;
    private Integer oldStatus;
    private Integer newStatus;
    private String details;
    private String operatorId;
    private String operatorName;
}
