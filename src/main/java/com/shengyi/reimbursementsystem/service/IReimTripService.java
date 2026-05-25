package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimTripDTO;
import com.shengyi.reimbursementsystem.entity.ReimTrip;

import java.util.Map;

public interface IReimTripService extends IService<ReimTrip> {
    /**
     * 保存行程
     * @param tripDTO 行程数据
     * @return 包含行程ID和补助ID的Map
     */
    Map<String, String> saveTrip(ReimTripDTO tripDTO);
    /**
     * 删除行程
     * @param tripId 行程ID
     */
    void deleteTrip(String tripId);
}