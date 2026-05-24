package com.shengyi.reimbursementsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shengyi.reimbursementsystem.common.Result;
import com.shengyi.reimbursementsystem.dto.ReimPageQueryDTO;
import com.shengyi.reimbursementsystem.dto.ReimSaveDTO;
import com.shengyi.reimbursementsystem.dto.ReimSubmitDTO;
import com.shengyi.reimbursementsystem.service.IReimMainService;
import com.shengyi.reimbursementsystem.vo.ReimMainVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "报销单主单管理", description = "报销单查询、保存与提交")
public class ReimMainController {

    private final IReimMainService reimMainService;

    @PostMapping("/REIM_QueryPageList")
    @Operation(summary = "查询报销单分页列表")
    public Result<IPage<ReimMainVO>> queryPageList(@Validated @RequestBody ReimPageQueryDTO dto) {
        IPage<ReimMainVO> pageResult = reimMainService.queryPageList(dto);
        return Result.success(pageResult);
    }

    @PostMapping("/REIM_Save")
    @Operation(summary = "保存报销单(含级联保存)")
    public Result<?> saveReim(@Validated @RequestBody ReimSaveDTO dto) {
        reimMainService.saveReimMain(dto);
        return Result.success();
    }

    @PostMapping("/REIM_Submit")
    @Operation(summary = "提交报销单")
    public Result<?> submitReim(@Validated @RequestBody ReimSubmitDTO dto) {
        reimMainService.submitReim(dto);
        return Result.success();
    }
}
