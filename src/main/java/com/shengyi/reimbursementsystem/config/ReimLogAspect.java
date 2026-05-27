package com.shengyi.reimbursementsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shengyi.reimbursementsystem.dto.ReimSubmitDTO;
import com.shengyi.reimbursementsystem.entity.ReimLog;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import com.shengyi.reimbursementsystem.mapper.ReimLogMapper;
import com.shengyi.reimbursementsystem.service.IReimMainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import com.shengyi.reimbursementsystem.service.impl.ReimMainServiceImpl;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ReimLogAspect {

    private final IReimMainService reimMainService;
    private final ReimLogMapper reimLogMapper;
    private final ObjectMapper objectMapper;

    @Around("execution(* ReimMainServiceImpl.submitReim(..)) || " +
            "execution(* ReimMainServiceImpl.updateStatus(..)) || " +
            "execution(* ReimMainServiceImpl.cancelReim(..))")
    public Object logStatusChange(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        String methodName = pjp.getSignature().getName();
        String reimId = null;
        String actionName = "状态变更";
        
        if ("submitReim".equals(methodName)) {
            if (args != null && args.length > 0 && args[0] instanceof ReimSubmitDTO) {
                reimId = ((ReimSubmitDTO) args[0]).getId();
            }
            actionName = "报销单提交";
        } else if ("updateStatus".equals(methodName) || "cancelReim".equals(methodName)) {
            if (args != null && args.length > 0 && args[0] instanceof String) {
                reimId = (String) args[0];
            }
            if ("updateStatus".equals(methodName)) actionName = "报销单状态更新(审批回调)";
            if ("cancelReim".equals(methodName)) actionName = "报销单作废";
        }

        ReimMain oldMain = null;
        if (reimId != null) {
            oldMain = reimMainService.getById(reimId);
        }

        // 执行目标方法
        Object result = pjp.proceed();

        if (reimId != null && oldMain != null) {
            try {
                ReimMain newMain = reimMainService.getById(reimId);
                if (newMain != null && !newMain.getReimStatus().equals(oldMain.getReimStatus())) {
                    ReimLog reimLog = new ReimLog();
                    reimLog.setReimId(reimId);
                    reimLog.setAction(actionName);
                    reimLog.setOldStatus(oldMain.getReimStatus());
                    reimLog.setNewStatus(newMain.getReimStatus());
                    
                    Map<String, Object> details = new HashMap<>();
                    details.put("reimNo", newMain.getReimNo());
                    // 移除硬编码脱敏，保留真实数据，由后续 @JsonEncrypt 功能统一在序列化层进行脱敏处理
                    details.put("reimburserName", newMain.getReimburserName());
                    details.put("subsidyTotal", newMain.getSubsidyTotal());
                    
                    reimLog.setDetails(objectMapper.writeValueAsString(details));
                    reimLog.setOperatorId(newMain.getUpdateUserId());
                    reimLog.setOperatorName(newMain.getUpdateUserName());
                    
                    reimLogMapper.insert(reimLog);
                }
            } catch (Exception e) {
                log.error("保存报销单状态变更日志失败", e);
            }
        }
        
        return result;
    }
}
