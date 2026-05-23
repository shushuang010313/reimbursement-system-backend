package com.shengyi.reimbursementsystem.controller;

import com.shengyi.reimbursementsystem.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.Map;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "费用分摊管理", description = "费用分摊相关接口")
public class ReimSplitController {

    @PostMapping("/REIM_SplitCalc")
    @Operation(summary = "重新计算分摊金额")
    public Result<?> splitCalc(@RequestBody Map<String, Object> params) {
        // TODO: 交由开发A完成
        return Result.success();
    }
}
