package com.shengyi.reimbursementsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shengyi.reimbursementsystem.dto.ReimSubsidyDTO;
import com.shengyi.reimbursementsystem.entity.ReimSubsidy;

import java.util.List;

public interface IReimSubsidyService extends IService<ReimSubsidy> {
    /**
     * 生成补助
     * @param tripId 补录行程ID
     * @param reimId 补录行程ID
     * @return 补助ID
     */
    String generateSubsidy(String tripId, String reimId);

    /**
     * 获取补助
     * @param reimId 补录行程ID
     * @return 补助列表
     */
    List<ReimSubsidy> getSubsidyByReimId(String reimId);

    /**
     * 更新补助金额
     * @param subsidyId 补助ID
     */
    void updateSubsidyAmount(String subsidyId);
}