package com.shengyi.reimbursementsystem.controller;

import com.shengyi.reimbursementsystem.common.Result;
import com.shengyi.reimbursementsystem.entity.ReimSubsidy;
import com.shengyi.reimbursementsystem.service.IReimSubsidyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "补助管理", description = "补助操作")
public class ReimSubsidyController {

    private final IReimSubsidyService reimSubsidyService;

    @GetMapping("/REIM_ListSubsidies")
    @Operation(summary = "查询补助列表")
    public Result<List<ReimSubsidy>> listSubsidies(@RequestParam("reimId") String reimId) {
        return Result.success(reimSubsidyService.getSubsidyByReimId(reimId));
    }
}
