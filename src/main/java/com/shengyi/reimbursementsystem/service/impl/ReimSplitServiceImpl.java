package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.entity.ReimSplit;
import com.shengyi.reimbursementsystem.mapper.ReimSplitMapper;
import com.shengyi.reimbursementsystem.service.IReimSplitService;
import org.springframework.stereotype.Service;

@Service
public class ReimSplitServiceImpl extends ServiceImpl<ReimSplitMapper, ReimSplit> implements IReimSplitService {
}
