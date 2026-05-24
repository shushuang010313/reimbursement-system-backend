package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimTripDTO;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.vo.ReimTripVO;

public interface IReimTripService extends IService<ReimTrip> {

    ReimTripVO saveTrip(ReimTripDTO dto);

    void deleteTrip(String tripId);
}
