package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimSubsidyDTO;
import com.shengyi.reimbursementsystem.entity.ReimSubsidy;

import java.util.List;

public interface IReimSubsidyService extends IService<ReimSubsidy> {
    void generateSubsidy(String tripId, String reimId);
    
    List<ReimSubsidy> getSubsidyByReimId(String reimId);
    
    void updateSubsidyAmount(String subsidyId);
}