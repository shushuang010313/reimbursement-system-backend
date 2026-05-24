package com.shengyi.reimbursementsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.dto.ReimSplitDTO;
import com.shengyi.reimbursementsystem.entity.ReimSplit;
import com.shengyi.reimbursementsystem.exception.BusinessException;
import com.shengyi.reimbursementsystem.mapper.ReimSplitMapper;
import com.shengyi.reimbursementsystem.service.IReimSplitService;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import com.shengyi.reimbursementsystem.mapper.ReimMainMapper;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReimSplitServiceImpl extends ServiceImpl<ReimSplitMapper, ReimSplit> implements IReimSplitService {

    private final ReimMainMapper reimMainMapper;

    /**
     * 计算并校验费用分摊比例与金额
     *
     * 核心算法逻辑：
     * 1. 业务上要求分摊比例总和严格等于 100%（即 1.0）。
     * 2. 为了防止前端或后端在计算小数时出现由于无限循环小数或精度丢失导致最终总额不等于报销单总金额的情况，
     *    我们采用“倒挤法”：明确规定第1行不可由用户直接编辑比例，而是系统通过 100% 减去其余所有行之和来推导得出。
     * 3. 具体金额的计算同理：用主单总金额减去其余所有行的具体金额之和，得到第1行的兜底金额，以此保证金额一分不差。
     *
     * @param reimId 报销主单ID，用于查询单据总金额
     * @param splitList 前端传入的分摊明细列表
     */
    @Override
    public void calculateSplitRatio(String reimId, List<ReimSplitDTO> splitList) {
        // 1. 基础参数校验
        if (splitList == null || splitList.isEmpty() || reimId == null) {
            return;
        }
        
        // 2. 查询主单信息，获取本次报销单需要分摊的总金额
        ReimMain main = reimMainMapper.selectById(reimId);
        if (main == null || main.getSubsidyTotal() == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "未找到报销主单或报销总金额为空");
        }
        BigDecimal totalAmount = main.getSubsidyTotal();
        
        // 用于累加除第1行之外的所有比例之和
        BigDecimal totalOtherRatio = BigDecimal.ZERO;
        // 用于累加除第1行之外的所有分摊金额之和
        BigDecimal totalOtherAmount = BigDecimal.ZERO;
        
        // 引用指向第1行对象，方便最后兜底计算
        ReimSplitDTO firstRow = null;

        // 3. 遍历分摊列表，分开处理第1行和其他行
        for (ReimSplitDTO dto : splitList) {
            if (dto.getSortNo() != null && dto.getSortNo() == 1) {
                // 暂存第1行，跳过当前循环的计算
                firstRow = dto;
            } else {
                if (dto.getSplitRatio() != null) {
                    // 累加其他行的比例
                    totalOtherRatio = totalOtherRatio.add(dto.getSplitRatio());
                    
                    // 计算当前行的具体分摊金额：单行金额 = 总金额 * 该行比例
                    // 精度控制：保留两位小数，并使用四舍五入 (HALF_UP) 策略
                    BigDecimal amount = totalAmount.multiply(dto.getSplitRatio()).setScale(2, RoundingMode.HALF_UP);
                    dto.setSplitAmount(amount);
                    
                    // 累加其他行的金额之和
                    totalOtherAmount = totalOtherAmount.add(amount);
                }
            }
        }

        // 4. 双重校验：前端传来且被累加的其他行比例总和，绝不能大于 1 (即 100%)
        if (totalOtherRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCodeEnum.REIM_004);
        }

        // 5. 使用“倒挤法”计算第1行的比例和具体金额，严防精度丢失
        if (firstRow != null) {
            // 第1行比例 = 1 - (其余行比例之和)
            BigDecimal firstRatio = BigDecimal.ONE.subtract(totalOtherRatio);
            // 精度控制：比例最高保留4位小数
            firstRatio = firstRatio.setScale(4, RoundingMode.HALF_UP);
            firstRow.setSplitRatio(firstRatio);
            
            // 第1行具体金额 = 报销总金额 - (其余行金额之和)
            BigDecimal firstAmount = totalAmount.subtract(totalOtherAmount);
            firstRow.setSplitAmount(firstAmount);
        }
    }
}
