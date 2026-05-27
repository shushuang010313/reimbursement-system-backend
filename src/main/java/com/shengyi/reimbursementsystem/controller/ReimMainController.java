package com.shengyi.reimbursementsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shengyi.reimbursementsystem.common.Result;
import com.shengyi.reimbursementsystem.dto.ReimPageQueryDTO;
import com.shengyi.reimbursementsystem.dto.ReimSaveDTO;
import com.shengyi.reimbursementsystem.dto.ReimSubmitDTO;
import com.shengyi.reimbursementsystem.service.AsyncExportService;
import com.shengyi.reimbursementsystem.service.IReimMainService;
import com.shengyi.reimbursementsystem.vo.ReimMainVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.shengyi.reimbursementsystem.annotation.Idempotent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "报销单主单管理", description = "报销单查询、保存与提交")
public class ReimMainController {

    private final IReimMainService reimMainService;
    private final AsyncExportService asyncExportService;

    /**
     * 查询报销单分页列表
     * @param dto 分页查询条件
     * @return 分页结果
     */
    @PostMapping("/REIM_QueryPageList")
    @Operation(summary = "查询报销单分页列表")
    public Result<IPage<ReimMainVO>> queryPageList(@Validated @RequestBody ReimPageQueryDTO dto) {
        IPage<ReimMainVO> pageResult = reimMainService.queryPageList(dto);
        return Result.success(pageResult);
    }

    /**
     * 保存报销单(含级联保存)
     * @param dto 报销单主单及明细数据
     * @return 统一返回结果
     */
    @PostMapping("/REIM_Save")
    @Operation(summary = "保存报销单(含级联保存)")
    @Idempotent(timeout = 5, message = "正在保存报销单，请勿频繁点击")
    public Result<java.util.Map<String, String>> saveReim(@Validated @RequestBody ReimSaveDTO dto) {
        String reimId = reimMainService.saveReimMain(dto);
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("id", reimId);
        return Result.success(data);
    }

    /**
     * 提交报销单
     * @param dto 提交所需数据（含乐观锁版本号）
     * @return 统一返回结果
     */
    @PostMapping("/REIM_Submit")
    @Operation(summary = "提交报销单")
    @Idempotent(timeout = 5, message = "提交处理中，请不要重复提交")
    public Result<?> submitReim(@Validated @RequestBody ReimSubmitDTO dto) {
        reimMainService.submitReim(dto);
        return Result.success();
    }

    /**
     * 查询报销单详情
     * @param id 报销单ID
     * @return 统一返回结果
     */
    @GetMapping("/REIM_GetDetail")
    @Operation(summary = "查询报销单详情")
    public Result<com.shengyi.reimbursementsystem.entity.ReimMain> getDetail(@RequestParam("id") String id) {
        com.shengyi.reimbursementsystem.entity.ReimMain main = reimMainService.getById(id);
        return Result.success(main);
    }

    /**
     * 更新报销单状态
     * @param params 包含id, status, remark
     * @return 统一返回结果
     */
    @PostMapping("/REIM_UpdateStatus")
    @Operation(summary = "更新报销单状态")
    public Result<?> updateStatus(@RequestBody java.util.Map<String, Object> params) {
        String id = (String) params.get("id");
        Integer status = (Integer) params.get("status");
        
        if (id == null || status == null) {
            return Result.error(com.shengyi.reimbursementsystem.common.ErrorCodeEnum.PARAM_ERROR);
        }
        
        reimMainService.updateStatus(id, status);
        
        return Result.success();
    }

    /**
     * 作废报销单
     * @param params 包含报销单id
     * @return 统一返回结果
     */
    @PostMapping("/REIM_Cancel")
    @Operation(summary = "作废报销单")
    public Result<?> cancelReim(@RequestBody java.util.Map<String, String> params) {
        String id = params.get("id");
        if (id == null || id.isEmpty()) {
            return Result.error(com.shengyi.reimbursementsystem.common.ErrorCodeEnum.PARAM_ERROR);
        }
        reimMainService.cancelReim(id);
        return Result.success();
    }

    /**
     * 发起百万级报表异步导出
     */
    @PostMapping("/REIM_ExportAsync")
    @Operation(summary = "发起百万级报表异步导出")
    public Result<java.util.Map<String, String>> submitExportTask(@RequestBody(required = false) ReimPageQueryDTO queryDTO) {
        String taskId = java.util.UUID.randomUUID().toString().replace("-", "");
        // 触发异步执行，需要把当前主线程的 userId 传给异步线程
        asyncExportService.executeAsyncExport(taskId, queryDTO, com.shengyi.reimbursementsystem.common.UserContext.getUserId());
        
        java.util.Map<String, String> res = new java.util.HashMap<>();
        res.put("taskId", taskId);
        res.put("status", "SUBMITTED");
        
        return Result.success(res);
    }

    /**
     * 轮询导出任务状态与获取文件
     */
    @GetMapping("/REIM_ExportStatus")
    @Operation(summary = "轮询导出任务状态与获取文件")
    public Result<java.util.Map<String, Object>> getExportStatus(@RequestParam("taskId") String taskId) {
        java.util.Map<String, Object> statusObj = asyncExportService.getExportStatus(taskId);
        if (statusObj != null) {
            return Result.success(statusObj);
        }
        return Result.error(404, "任务不存在或已过期");
    }
}
