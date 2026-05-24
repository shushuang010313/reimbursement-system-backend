package com.shengyi.reimbursementsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.common.Result;
import com.shengyi.reimbursementsystem.dto.ReimCalendarDTO;
import com.shengyi.reimbursementsystem.entity.ReimCalendar;
import com.shengyi.reimbursementsystem.service.IReimCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "补助日历管理", description = "补助日历的查询与保存")
public class ReimCalendarController {

    private final IReimCalendarService reimCalendarService;
    private final ObjectMapper objectMapper;

    @PostMapping("/REIM_GetCalendar")
    @Operation(summary = "获取补助日历")
    public Result<List<ReimCalendar>> getCalendar(@RequestBody Map<String, Object> params) {
        String subsidyId = (String) params.get("subsidyId");
        if (subsidyId == null || subsidyId.isEmpty()) {
            return Result.error(ErrorCodeEnum.PARAM_ERROR);
        }
        List<ReimCalendar> calendarList = reimCalendarService.getCalendarBySubsidyId(subsidyId);
        return Result.success(calendarList);
    }

    @PostMapping("/REIM_SaveSubsidy")
    @Operation(summary = "保存补助日历状态")
    public Result<?> saveSubsidy(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> calendarListRaw = (List<Map<String, Object>>) params.get("calendarList");
            
            if (calendarListRaw == null || calendarListRaw.isEmpty()) {
                return Result.error(ErrorCodeEnum.PARAM_ERROR);
            }
            
            String subsidyId = (String) params.get("subsidyId");
            
            // 将 LinkedHashMap 转换为 ReimCalendarDTO
            List<ReimCalendarDTO> dtoList = calendarListRaw.stream()
                .map(map -> {
                    ReimCalendarDTO dto = objectMapper.convertValue(map, ReimCalendarDTO.class);
                    dto.setSubsidyId(subsidyId);
                    return dto;
                })
                .collect(Collectors.toList());
            
            reimCalendarService.updateCalendarStatus(dtoList);
            return Result.success();
        } catch (Exception e) {
            return Result.error(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }
}