package com.shengyi.reimbursementsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.shengyi.reimbursementsystem.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fk_reim_trip")
public class ReimTrip extends BaseEntity {

    private String reimId;
    private String travelerId;
    private String travelerNo;
    private String travelerName;
    private String departureCityId;
    private String departureCityName;
    private String arriveCityId;
    private String arriveCityName;
    private String arriveCityLevel;
    private LocalDate departureDate;
    private LocalDate arriveDate;
    private Integer tripDays;
    private String tripDesc;

    @TableLogic
    private Integer delFlag;
}
