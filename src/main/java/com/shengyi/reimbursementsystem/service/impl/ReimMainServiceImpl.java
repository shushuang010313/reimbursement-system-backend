package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import com.shengyi.reimbursementsystem.entity.ReimSubsidy;
import com.shengyi.reimbursementsystem.mapper.ReimMainMapper;
import com.shengyi.reimbursementsystem.service.IReimMainService;
import com.shengyi.reimbursementsystem.service.IReimSubsidyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReimMainServiceImpl extends ServiceImpl<ReimMainMapper, ReimMain> implements IReimMainService {

    private final IReimSubsidyService reimSubsidyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTotalAmount(String reimId) {
        // 查询该报销单下的所有补助记录
        List<ReimSubsidy> subsidies = reimSubsidyService.list(
                new LambdaQueryWrapper<ReimSubsidy>()
                        .eq(ReimSubsidy::getReimId, reimId)
        );

        // 计算总金额
        BigDecimal subsidyTotal = BigDecimal.ZERO;
        BigDecimal mealAllowance = BigDecimal.ZERO;
        BigDecimal transportationAllowance = BigDecimal.ZERO;
        BigDecimal phoneAllowance = BigDecimal.ZERO;

        for (ReimSubsidy subsidy : subsidies) {
            if (subsidy.getSubsidyAmount() != null) {
                subsidyTotal = subsidyTotal.add(subsidy.getSubsidyAmount());
            }
            if (subsidy.getMealSubsidy() != null) {
                mealAllowance = mealAllowance.add(subsidy.getMealSubsidy());
            }
            if (subsidy.getTransportSubsidy() != null) {
                transportationAllowance = transportationAllowance.add(subsidy.getTransportSubsidy());
            }
            if (subsidy.getPhoneSubsidy() != null) {
                phoneAllowance = phoneAllowance.add(subsidy.getPhoneSubsidy());
            }
        }

        // 更新主表
        ReimMain reimMain = this.getById(reimId);
        if (reimMain != null) {
            reimMain.setSubsidyTotal(subsidyTotal.setScale(2, RoundingMode.HALF_UP));
            reimMain.setMealAllowance(mealAllowance.setScale(2, RoundingMode.HALF_UP));
            reimMain.setTransportationAllowance(transportationAllowance.setScale(2, RoundingMode.HALF_UP));
            reimMain.setPhoneAllowance(phoneAllowance.setScale(2, RoundingMode.HALF_UP));
            this.updateById(reimMain);
        }
    }
}
