package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.entity.ReimSplit;

public interface IReimSplitService extends IService<ReimSplit> {
    void calculateSplitRatio(String reimId, java.util.List<com.shengyi.reimbursementsystem.dto.ReimSplitDTO> splitList);
}
