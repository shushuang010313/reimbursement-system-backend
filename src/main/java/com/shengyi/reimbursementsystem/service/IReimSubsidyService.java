package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.entity.ReimSubsidy;
import com.shengyi.reimbursementsystem.entity.ReimTrip;

public interface IReimSubsidyService extends IService<ReimSubsidy> {

    ReimSubsidy createSubsidyFromTrip(ReimTrip trip);
}
