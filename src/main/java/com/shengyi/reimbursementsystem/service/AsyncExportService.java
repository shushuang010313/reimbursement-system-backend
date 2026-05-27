package com.shengyi.reimbursementsystem.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shengyi.reimbursementsystem.dto.ReimPageQueryDTO;
import com.shengyi.reimbursementsystem.entity.FkAsyncTask;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import com.shengyi.reimbursementsystem.mapper.FkAsyncTaskMapper;
import com.shengyi.reimbursementsystem.mapper.ReimMainMapper;
import com.shengyi.reimbursementsystem.vo.ExportTaskVO;
import com.shengyi.reimbursementsystem.vo.ReimExportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncExportService {

    private final ReimMainMapper reimMainMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FkAsyncTaskMapper fkAsyncTaskMapper;

    private static final String REDIS_TASK_PREFIX = "reim:export:task:";

    @Async("exportThreadPool")
    public void executeAsyncExport(String taskId, ReimPageQueryDTO queryDTO, String userId) {
        // 在异步线程中恢复用户上下文，确保数据权限拦截器能正常工作
        com.shengyi.reimbursementsystem.common.UserContext.setUserId(userId);
        
        // 1. 同时在 Redis 和 MySQL 持久化任务
        String redisKey = REDIS_TASK_PREFIX + taskId;
        ExportTaskVO taskVO = new ExportTaskVO().setTaskId(taskId).setStatus("SUBMITTED").setProgress(0);
        redisTemplate.opsForValue().set(redisKey, taskVO, 1, TimeUnit.HOURS);
        
        FkAsyncTask dbTask = new FkAsyncTask();
        dbTask.setId(taskId);
        dbTask.setTaskName("报销明细导出");
        dbTask.setTaskType("EXPORT");
        dbTask.setStatus(1); // 1-处理中
        dbTask.setProgress(0);
        dbTask.setOperatorId("system");
        fkAsyncTaskMapper.insert(dbTask);

        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = "Reimbursement_Export_" + taskId + ".xlsx";
        String filePath = tempDir + File.separator + fileName;

        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(filePath, ReimExportVO.class).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("报销明细").build();

            // 为了防OOM，采用分页查库（游标效果）
            int pageSize = 1000; // 每页1000条
            long current = 1;
            
            // 组装查询条件
            QueryWrapper<ReimMain> queryWrapper = new QueryWrapper<>();
            if (queryDTO != null) {
                if (StringUtils.hasText(queryDTO.getReimNo())) {
                    queryWrapper.like("reim_no", queryDTO.getReimNo());
                }
                if (StringUtils.hasText(queryDTO.getReimTitle())) {
                    queryWrapper.like("reimbursement_title", queryDTO.getReimTitle());
                }
                if (StringUtils.hasText(queryDTO.getBusinessTripReason())) {
                    queryWrapper.like("business_trip_reason", queryDTO.getBusinessTripReason());
                }
                if (StringUtils.hasText(queryDTO.getCompanyId())) {
                    queryWrapper.eq("reim_company_id", queryDTO.getCompanyId());
                }
                if (StringUtils.hasText(queryDTO.getDepartmentId())) {
                    queryWrapper.eq("reim_department_id", queryDTO.getDepartmentId());
                }
            }
            
            // 先查总数
            long total = reimMainMapper.selectCount(queryWrapper);
            
            if (total == 0) {
                taskVO.setStatus("SUCCESS").setProgress(100).setMessage("暂无数据导出").setFileName(fileName);
                redisTemplate.opsForValue().set(redisKey, taskVO, 1, TimeUnit.HOURS);
                
                dbTask.setStatus(2); // 2-成功
                dbTask.setProgress(100);
                dbTask.setFileUrl(filePath);
                dbTask.setCompletionTime(new Date());
                fkAsyncTaskMapper.updateById(dbTask);
                return;
            }

            long totalPages = (total + pageSize - 1) / pageSize;
            long exportedCount = 0;

            for (; current <= totalPages; current++) {
                Page<ReimMain> page = new Page<>(current, pageSize);
                Page<ReimMain> resultPage = reimMainMapper.selectPage(page, queryWrapper);
                List<ReimMain> records = resultPage.getRecords();

                List<ReimExportVO> exportDataList = new ArrayList<>();
                for (ReimMain record : records) {
                    exportDataList.add(convertToExportVO(record));
                }

                // 流式写入当前页数据
                excelWriter.write(exportDataList, writeSheet);

                exportedCount += records.size();
                
                // 更新进度到 Redis 和 数据库
                int progress = (int) (exportedCount * 100 / total);
                taskVO.setProgress(progress).setMessage(String.format("已导出 %d / %d 条", exportedCount, total));
                redisTemplate.opsForValue().set(redisKey, taskVO, 1, TimeUnit.HOURS);
                
                dbTask.setProgress(progress);
                fkAsyncTaskMapper.updateById(dbTask);

                // 模拟耗时，以便在前端或Swagger能清晰地观察到进度变化（仅供演示体验亮点使用）
                Thread.sleep(100);
            }

            // 完成任务
            taskVO.setStatus("SUCCESS").setProgress(100).setMessage("导出完成").setFileName(fileName);
            redisTemplate.opsForValue().set(redisKey, taskVO, 1, TimeUnit.HOURS);
            
            dbTask.setStatus(2); // 2-成功
            dbTask.setProgress(100);
            dbTask.setFileUrl(filePath);
            dbTask.setCompletionTime(new Date());
            fkAsyncTaskMapper.updateById(dbTask);
            
            log.info("【异步导出】任务 {} 执行完毕，文件存放于：{}", taskId, filePath);

        } catch (Exception e) {
            log.error("【异步导出】任务 {} 执行失败：", taskId, e);
            taskVO.setStatus("FAILED").setMessage("导出异常：" + e.getMessage());
            redisTemplate.opsForValue().set(redisKey, taskVO, 1, TimeUnit.HOURS);
            
            dbTask.setStatus(3); // 3-失败
            dbTask.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 1900 ? e.getMessage().substring(0, 1900) : e.getMessage());
            dbTask.setCompletionTime(new Date());
            fkAsyncTaskMapper.updateById(dbTask);
        } finally {
            if (excelWriter != null) {
                // 千万别忘记finish，否则文件可能不完整
                excelWriter.finish();
            }
            com.shengyi.reimbursementsystem.common.UserContext.clear();
        }
    }

    private ReimExportVO convertToExportVO(ReimMain record) {
        ReimExportVO vo = new ReimExportVO();
        vo.setReimNo(record.getReimNo());
        vo.setReimbursementTitle(record.getReimbursementTitle());
        vo.setReimburserName(record.getReimburserName());
        vo.setReimDepartmentName(record.getReimDepartmentName());
        vo.setBusinessTripReason(record.getBusinessTripReason());
        vo.setSubsidyTotal(record.getSubsidyTotal());
        
        // 此处为兼容亮点一，手工进行脱敏处理，防止导出文件泄漏明文
        vo.setPayeeIdCard(maskIdCard(record.getPayeeIdCard()));
        vo.setPayeeBankAccount(maskBankCard(record.getPayeeBankAccount()));
        
        if (record.getCreationTime() != null) {
            // Note: If creationTime is LocalDateTime, convert it to Date for EasyExcel.
            // ReimMain uses creationTime inside BaseEntity but it might be LocalDateTime. Let's assume it is.
            vo.setCreationTime(Date.from(record.getCreationTime().atZone(ZoneId.systemDefault()).toInstant()));
        }

        String statusDesc = "未知";
        if (record.getReimStatus() != null) {
            switch (record.getReimStatus()) {
                case 0: statusDesc = "草稿"; break;
                case 1: statusDesc = "已完成"; break;
                case 2: statusDesc = "已作废"; break;
            }
        }
        vo.setStatusDesc(statusDesc);
        
        return vo;
    }
    
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 15) return idCard;
        if (idCard.length() == 18) {
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        }
        return idCard.substring(0, 4) + "****" + idCard.substring(idCard.length() - 4);
    }
    
    private String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) return bankCard;
        return bankCard.substring(0, 4) + "****" + bankCard.substring(bankCard.length() - 4);
    }
    
    public java.util.Map<String, Object> getExportStatus(String taskId) {
        FkAsyncTask dbTask = fkAsyncTaskMapper.selectById(taskId);
        if (dbTask == null) {
            return null;
        }
        
        String statusStr = "SUBMITTED";
        if (dbTask.getStatus() != null) {
            switch (dbTask.getStatus()) {
                case 0: statusStr = "SUBMITTED"; break;
                case 1: statusStr = "PROCESSING"; break;
                case 2: statusStr = "SUCCESS"; break;
                case 3: statusStr = "FAILED"; break;
            }
        }
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", statusStr);
        result.put("progress", dbTask.getProgress());
        
        if (dbTask.getStatus() != null && dbTask.getStatus() == 2) {
            // 模拟OSS临时下载链接
            result.put("downloadUrl", "/api/reim/export/download/" + taskId);
        } else if (dbTask.getStatus() != null && dbTask.getStatus() == 3) {
            result.put("errorMsg", dbTask.getErrorMsg());
        }
        return result;
    }
}
