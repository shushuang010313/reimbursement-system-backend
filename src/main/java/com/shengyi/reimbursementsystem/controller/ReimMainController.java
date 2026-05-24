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
        
        com.shengyi.reimbursementsystem.entity.ReimMain updateMain = new com.shengyi.reimbursementsystem.entity.ReimMain();
        updateMain.setId(id);
        updateMain.setReimStatus(status);
        reimMainService.updateById(updateMain);
        
        return Result.success();
    }
}
