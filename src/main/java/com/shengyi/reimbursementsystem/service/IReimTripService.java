package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimTripDTO;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import java.util.Map;

public interface IReimTripService extends IService<ReimTrip> {
    Map<String, String> saveTrip(ReimTripDTO tripDTO);
    
    void deleteTrip(String tripId);
}