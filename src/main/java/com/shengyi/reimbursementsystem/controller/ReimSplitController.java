package com.shengyi.reimbursementsystem.controller;

import com.shengyi.reimbursementsystem.common.Result;
import com.shengyi.reimbursementsystem.dto.ReimSplitDTO;
import com.shengyi.reimbursementsystem.service.IReimSplitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "费用分摊管理", description = "费用分摊相关接口")
public class ReimSplitController {

    private final IReimSplitService reimSplitService;

    /**
     * 重新计算分摊比例与金额
     *
     * 当前端修改除第1行之外的任意一行的分摊比例时，调用此接口进行重新计算。
     * 后端将基于"倒挤法"算出第1行的比例和具体金额，确保总和严格等于100%且金额等于主单总金额。
     *
     * @param reimId 报销主单ID
     * @param splitList 待计算的分摊明细列表
     * @return 计算并补齐了第1行比例和金额的列表
     */
    @PostMapping("/REIM_SplitCalc/{reimId}")
    @Operation(summary = "重新计算分摊比例")
    public Result<List<ReimSplitDTO>> splitCalc(@PathVariable("reimId") String reimId, @RequestBody List<ReimSplitDTO> splitList) {
        reimSplitService.calculateSplitRatio(reimId, splitList);
        return Result.success(splitList);
    }
}
