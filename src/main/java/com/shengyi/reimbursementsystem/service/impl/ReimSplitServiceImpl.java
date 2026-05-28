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
     * 【学习指引】计算并校验费用分摊比例与金额（经典倒挤算法）
     *
     * 【学习指引】核心算法背景：
     * 1. 业务上要求分摊比例总和严格等于 100%（即 1.0）。
     * 2. 避免“除不尽”难题：如果总额100元平分3份，每份33.33，总计99.99，会丢失0.01元。
     * 3. 破局之道——“倒挤法”：人为规定第1行永远是“兜底行”，不可编辑。它的值 = 100% - 其余所有行之和。
     *    同理，金额也等于：总金额 - 其余行金额之和。以此保证最终金额一分不差，严丝合缝。
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
        
        // 【学习指引】查询出当前要分摊的主单据，拿到非常关键的“总金额”
        ReimMain main = reimMainMapper.selectById(reimId);
        if (main == null || main.getSubsidyTotal() == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "未找到报销主单或报销总金额为空");
        }
        BigDecimal totalAmount = main.getSubsidyTotal();
        
        // 【学习指引】累加器初始化：用于把第 2,3,4... 行的比例和金额都加起来
        BigDecimal totalOtherRatio = BigDecimal.ZERO;
        BigDecimal totalOtherAmount = BigDecimal.ZERO;
        
        // 【学习指引】保留一个引用，专门指向第1行，等别人都算完了，最后拿它来兜底
        ReimSplitDTO firstRow = null;

        // 3. 遍历分摊列表，分开处理第1行和其他行
        for (ReimSplitDTO dto : splitList) {
            if (dto.getSortNo() != null && dto.getSortNo() == 1) {
                // 【学习指引】如果是第1行，什么都不算，只把它暂存起来
                firstRow = dto;
            } else {
                if (dto.getSplitRatio() != null) {
                    // 【学习指引】如果是其他行，将其比例滚入总和
                    totalOtherRatio = totalOtherRatio.add(dto.getSplitRatio());
                    
                    // 【学习指引】正向计算其他行的金额：总金额 * 该行比例（使用四舍五入保留2位小数）
                    BigDecimal amount = totalAmount.multiply(dto.getSplitRatio()).setScale(2, RoundingMode.HALF_UP);
                    dto.setSplitAmount(amount);
                    
                    // 【学习指引】将算出来的金额也滚入总和
                    totalOtherAmount = totalOtherAmount.add(amount);
                }
            }
        }

        // 【学习指引】安全拦截：其他行加起来就已经超过 100% 了，那第1行就要变成负数了，这是绝对不允许的
        if (totalOtherRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCodeEnum.REIM_004);
        }

        // 【学习指引】最精彩的倒挤计算环节开始
        if (firstRow != null) {
            // 【学习指引】第1行比例 = 1.0 - (其余所有行比例之和)
            BigDecimal firstRatio = BigDecimal.ONE.subtract(totalOtherRatio);
            // 比例最高保留4位小数
            firstRatio = firstRatio.setScale(4, RoundingMode.HALF_UP);
            firstRow.setSplitRatio(firstRatio);
            
            // 【学习指引】第1行金额 = 报销总金额 - (其余所有行金额之和)
            BigDecimal firstAmount = totalAmount.subtract(totalOtherAmount);
            firstRow.setSplitAmount(firstAmount);
        }
    }
}
