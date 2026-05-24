package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimTripDTO;
import com.shengyi.reimbursementsystem.entity.ReimTrip;

public interface IReimTripService extends IService<ReimTrip> {
    java.util.Map<String, String> saveTrip(ReimTripDTO tripDTO);
    
    void deleteTrip(String tripId);
}