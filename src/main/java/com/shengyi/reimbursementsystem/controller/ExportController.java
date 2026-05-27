package com.shengyi.reimbursementsystem.controller;

import com.shengyi.reimbursementsystem.common.Result;
import com.shengyi.reimbursementsystem.dto.ReimPageQueryDTO;
import com.shengyi.reimbursementsystem.service.AsyncExportService;
import com.shengyi.reimbursementsystem.vo.ExportTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Tag(name = "报销单导出接口", description = "用于百万级防OOM的大数据异步导出")
@RestController
@RequestMapping("/reim/export")
@RequiredArgsConstructor
public class ExportController {

    private final AsyncExportService asyncExportService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_TASK_PREFIX = "reim:export:task:";

    @Operation(summary = "提交异步导出任务")
    @PostMapping("/async")
    public Result<java.util.Map<String, String>> submitExportTask(@RequestBody(required = false) ReimPageQueryDTO queryDTO) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        // 异步执行导出，必须传递当前主线程的 user-id
        asyncExportService.executeAsyncExport(taskId, queryDTO, com.shengyi.reimbursementsystem.common.UserContext.getUserId());
        
        java.util.Map<String, String> res = new java.util.HashMap<>();
        res.put("taskId", taskId);
        res.put("status", "已提交");
        
        return Result.success(res);
    }

    @Operation(summary = "查询导出进度与状态")
    @GetMapping("/status/{taskId}")
    public Result<java.util.Map<String, Object>> getExportStatus(@PathVariable String taskId) {
        java.util.Map<String, Object> statusObj = asyncExportService.getExportStatus(taskId);
        if (statusObj != null) {
            return Result.success(statusObj);
        }
        return Result.error(404, "任务不存在或已过期");
    }

    @Operation(summary = "下载已生成的 Excel 文件")
    @GetMapping("/download/{taskId}")
    public ResponseEntity<FileSystemResource> downloadFile(@PathVariable String taskId) {
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
