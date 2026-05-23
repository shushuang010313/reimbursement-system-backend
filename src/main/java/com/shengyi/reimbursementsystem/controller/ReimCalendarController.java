package com.shengyi.reimbursementsystem.controller;

import com.shengyi.reimbursementsystem.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "补助日历管理", description = "补助日历的查询与保存")
public class ReimCalendarController {

    @PostMapping("/REIM_GetCalendar")
    @Operation(summary = "获取补助日历")
    public Result<?> getCalendar(@RequestBody Map<String, Object> params) {
        // TODO: 交由开发B完成
        return Result.success();
    }

    @PostMapping("/REIM_SaveSubsidy")
    @Operation(summary = "保存补助日历状态")
    public Result<?> saveSubsidy(@RequestBody Map<String, Object> params) {
        // TODO: 交由开发B完成
        return Result.success();
    }
}
