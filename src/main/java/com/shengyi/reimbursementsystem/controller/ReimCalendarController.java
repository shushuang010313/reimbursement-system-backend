package com.shengyi.reimbursementsystem.controller;

import com.shengyi.reimbursementsystem.common.Result;
import com.shengyi.reimbursementsystem.dto.ReimGetCalendarRequestDTO;
import com.shengyi.reimbursementsystem.dto.ReimSaveSubsidyRequestDTO;
import com.shengyi.reimbursementsystem.service.IReimCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "补助日历管理", description = "补助日历的查询与保存")
public class ReimCalendarController {

    private final IReimCalendarService reimCalendarService;

    @PostMapping("/REIM_GetCalendar")
    @Operation(summary = "获取补助日历")
    public Result<?> getCalendar(@Validated @RequestBody ReimGetCalendarRequestDTO dto) {
        return Result.success(reimCalendarService.getCalendarBySubsidyId(dto.getSubsidyId()));
    }

    @PostMapping("/REIM_SaveSubsidy")
    @Operation(summary = "保存补助日历状态")
    public Result<?> saveSubsidy(@Validated @RequestBody ReimSaveSubsidyRequestDTO dto) {
        reimCalendarService.updateCalendarStatus(dto.getSubsidyId(), dto.getCalendarList());
        return Result.success();
    }
}
