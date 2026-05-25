package com.shengyi.reimbursementsystem.controller;

import com.shengyi.reimbursementsystem.common.Result;
import com.shengyi.reimbursementsystem.dto.ReimTripDTO;
import com.shengyi.reimbursementsystem.service.IReimTripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "补录行程管理", description = "补录行程操作")
public class ReimTripController {

    private final IReimTripService reimTripService;

    /**
     * 保存补录行程
     * @param dto 补录行程DTO
     * @return
     */
    @PostMapping("/REIM_SaveTrip")
    @Operation(summary = "保存补录行程")
    public Result<java.util.Map<String, String>> saveTrip(@Valid @RequestBody ReimTripDTO dto) {
        java.util.Map<String, String> data = reimTripService.saveTrip(dto);
        return Result.success(data);
    }

    /**
     * 删除补录行程
     * @param tripId 补录行程ID
     * @return
     */
    @PostMapping("/REIM_DeleteTrip")
    @Operation(summary = "删除补录行程")
    public Result<?> deleteTrip(@RequestParam("tripId") String tripId) {
        reimTripService.deleteTrip(tripId);
        return Result.success();
    }
}