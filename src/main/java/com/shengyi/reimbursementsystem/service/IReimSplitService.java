package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.entity.ReimSplit;
import com.shengyi.reimbursementsystem.dto.ReimSplitDTO;
import java.util.List;

public interface IReimSplitService extends IService<ReimSplit> {
    void calculateSplitRatio(String reimId, List<ReimSplitDTO> splitList);
}
