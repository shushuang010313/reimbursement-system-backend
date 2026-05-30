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

    // 【答辩重点】为什么要用 AOP 做异动日志？
    // 如果评委问：为什么不在业务代码里直接写 insert 日志？
    // 答：为了实现“业务逻辑”与“系统日志”的解耦。如果我们把记日志的逻辑写死在 Service 里，会导致代码非常臃肿且难以维护。
    // 使用 Spring AOP (@Aspect + @Around)，我们可以在不修改原有主流程代码的情况下，透明地切入方法执行前后，获取老状态和新状态进行对比入库。

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

        // 执行目标方法 (业务侧真正执行提交/作废的地方)
        Object result = pjp.proceed();

        if (reimId != null && oldMain != null) {
            try {
                // 【答辩重点】如何记录“状态的变动”？
                // 答：利用 Around 环绕通知的特性，在 proceed() 之前查一次数据库拿到老状态 oldMain，
                // 在 proceed() 之后再查一次拿到新状态 newMain。如果状态发生不一致，说明流转成功，才构造 ReimLog 实体进行落库。
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
