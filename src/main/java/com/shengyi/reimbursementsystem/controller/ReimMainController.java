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
import com.shengyi.reimbursementsystem.common.ErrorCodeEnum;
import com.shengyi.reimbursementsystem.common.UserContext;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.shengyi.reimbursementsystem.vo.ExportTaskVO;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/fccapi")
@RequiredArgsConstructor
@Tag(name = "报销单主单管理", description = "报销单查询、保存与提交")
public class ReimMainController {

    private final IReimMainService reimMainService;
    private final AsyncExportService asyncExportService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String REDIS_TASK_PREFIX = "reim:export:task:";

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
     * 【学习指引】这是保存报销单的入口 API。
     * @param dto 报销单主单及明细数据
     * @return 统一返回结果，其中 data 包含生成的报销单主键 ID
     */
    @PostMapping("/REIM_Save")
    @Operation(summary = "保存报销单(含级联保存)")
    // 【学习指引】@Idempotent 用于防重复提交，5秒内相同的请求会被拦截，防止用户连续快速点击产生重复数据
    @Idempotent(timeout = 5, message = "正在保存报销单，请勿频繁点击")
    // 【学习指引】@Validated 会对 DTO 中的属性进行 JSR303 校验（如判空、长度限制等）
    public Result<Map<String, String>> saveReim(@Validated @RequestBody ReimSaveDTO dto) {
        // 【学习指引】调用 Service 层进行核心业务逻辑处理，返回生成的报销单主键ID
        String reimId = reimMainService.saveReimMain(dto);
        Map<String, String> data = new HashMap<>();
        data.put("id", reimId);
        // 【学习指引】统一封装成 Result 对象返回给前端
        return Result.success(data);
    }

    /**
     * 提交报销单
     * 【学习指引】这是提交报销单的入口 API。提交操作会涉及状态流转和与外部 BPM 系统的交互。
     * @param dto 提交所需数据（含乐观锁版本号）
     * @return 统一返回结果
     */
    @PostMapping("/REIM_Submit")
    @Operation(summary = "提交报销单")
    // 【学习指引】@Idempotent 用于防重复提交拦截
    @Idempotent(timeout = 5, message = "提交处理中，请不要重复提交")
    public Result<?> submitReim(@Validated @RequestBody ReimSubmitDTO dto) {
        // 【学习指引】调用 Service 执行复杂的提交业务逻辑（分布式锁、最终一致性）
        reimMainService.submitReim(dto);
        return Result.success();
    }

    /**
     * 查询报销单详情
     * 【学习指引】这是获取单据详情的简单接口。虽然代码只有寥寥几行，但底层依赖了 MyBatis-Plus 和自定义注解的诸多特性。
     * @param id 报销单ID
     * @return 统一返回结果
     */
    @GetMapping("/REIM_GetDetail")
    @Operation(summary = "查询报销单详情")
    public Result<ReimMain> getDetail(@RequestParam("id") String id) {
        // 【学习指引】调用 MyBatis-Plus 提供的通用 getById 方法。
        // 隐式特性 1：自动附加逻辑删除过滤（如 WHERE del_flag = 0），具体见实体类的 @TableLogic 注解。
        // 隐式特性 2：如果配置了数据权限拦截器（DataPermissionInterceptor），底层会在拼装 SQL 时自动加上权限范围的条件过滤。
        ReimMain main = reimMainService.getById(id);
        
        // 【学习指引】返回实体给前端时，Spring MVC 会执行 JSON 序列化。
        // 隐式特性 3：注意看 ReimMain 实体中的 @JsonEncrypt 注解，序列化时会自动触发脱敏逻辑，将敏感字段（如身份证、银行卡）打码。
        return Result.success(main);
    }

    /**
     * 更新报销单状态
     * @param params 包含id, status, remark
     * @return 统一返回结果
     */
    @PostMapping("/REIM_UpdateStatus")
    @Operation(summary = "更新报销单状态")
    public Result<?> updateStatus(@RequestBody Map<String, Object> params) {
        String id = (String) params.get("id");
        Integer status = (Integer) params.get("status");
        
        if (id == null || status == null) {
            return Result.error(ErrorCodeEnum.PARAM_ERROR);
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
    public Result<?> cancelReim(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        if (id == null || id.isEmpty()) {
            return Result.error(ErrorCodeEnum.PARAM_ERROR);
        }
        reimMainService.cancelReim(id);
        return Result.success();
    }

    /**
     * 发起百万级报表异步导出
     */
    @PostMapping("/REIM_ExportAsync")
    @Operation(summary = "发起百万级报表异步导出")
    public Result<Map<String, String>> submitExportTask(@RequestBody(required = false) ReimPageQueryDTO queryDTO) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        // 触发异步执行，需要把当前主线程的 userId 传给异步线程
        asyncExportService.executeAsyncExport(taskId, queryDTO, UserContext.getUserId());
        
        Map<String, String> res = new HashMap<>();
        res.put("taskId", taskId);
        res.put("status", "SUBMITTED");
        
        return Result.success(res);
    }

    /**
     * 轮询导出任务状态与获取文件
     */
    @GetMapping("/REIM_ExportStatus")
    @Operation(summary = "轮询导出任务状态与获取文件")
    public Result<Map<String, Object>> getExportStatus(@RequestParam("taskId") String taskId) {
        // 【学习指引】调用底层的统一查库服务，返回进度（0-100）和任务状态（SUCCESS/PROCESSING等）
        Map<String, Object> statusObj = asyncExportService.getExportStatus(taskId);
        if (statusObj != null) {
            return Result.success(statusObj);
        }
        return Result.error(404, "任务不存在或已过期");
    }

    /**
     * 获取文件（模拟OSS下载）
     */
    @GetMapping("/REIM_ExportDownload")
    @Operation(summary = "下载已生成的 Excel 文件")
    public ResponseEntity<FileSystemResource> downloadFile(@RequestParam("taskId") String taskId) {
        String redisKey = REDIS_TASK_PREFIX + taskId;
        Object taskObj = redisTemplate.opsForValue().get(redisKey);
        if (taskObj instanceof ExportTaskVO) {
            ExportTaskVO taskVO = (ExportTaskVO) taskObj;
            if ("SUCCESS".equals(taskVO.getStatus()) && taskVO.getFileName() != null) {
                String tempDir = System.getProperty("java.io.tmpdir");
                File file = new File(tempDir + File.separator + taskVO.getFileName());
                if (file.exists()) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(taskVO.getFileName(), StandardCharsets.UTF_8));
                    
                    return ResponseEntity
                            .ok()
                            .headers(headers)
                            .contentLength(file.length())
                            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                            .body(new FileSystemResource(file));
                }
            }
        }
        throw new RuntimeException("文件尚未生成或已过期");
    }
}
