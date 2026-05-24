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

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ReimLogAspect {

    private final IReimMainService reimMainService;
    private final ReimLogMapper reimLogMapper;
    private final ObjectMapper objectMapper;

    @Around("execution(* com.shengyi.reimbursementsystem.service.impl.ReimMainServiceImpl.submitReim(..))")
    public Object logStatusChange(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        String reimId = null;
        if (args != null && args.length > 0 && args[0] instanceof ReimSubmitDTO) {
            reimId = ((ReimSubmitDTO) args[0]).getId();
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
                    reimLog.setAction("报销单提交");
                    reimLog.setOldStatus(oldMain.getReimStatus());
                    reimLog.setNewStatus(newMain.getReimStatus());
                    
                    Map<String, Object> details = new HashMap<>();
                    details.put("reimNo", newMain.getReimNo());
                    // 敏感字段打码
                    String name = newMain.getReimburserName();
                    if (name != null && name.length() > 1) {
                        name = name.substring(0, 1) + "**";
                    }
                    details.put("reimburserNameMasked", name);
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
