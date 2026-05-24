package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.entity.ReimTrip;
import com.shengyi.reimbursementsystem.mapper.ReimTripMapper;
import com.shengyi.reimbursementsystem.service.IReimTripService;
import org.springframework.stereotype.Service;

@Service
public class ReimTripServiceImpl extends ServiceImpl<ReimTripMapper, ReimTrip> implements IReimTripService {
}
