package com.shengyi.reimbursementsystem.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shengyi.reimbursementsystem.entity.ReimMain;
import com.shengyi.reimbursementsystem.service.IReimMainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class BpmCompensationJob {

    private final IReimMainService reimMainService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 定时任务：扫描状态为已完成（待推送）且超5分钟未处理的数据
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void compensateBpmPush() {
        log.info("开始执行BPM审批流推送补偿任务...");
        
        LocalDateTime fiveMinsAgo = LocalDateTime.now().minusMinutes(5);
        
        // 查找状态为 1（已完成/待推送）并且更新时间在5分钟之前的数据
        // 实际业务中应有 push_status 字段，这里假设通过 Redis 记录是否已成功推送来防止重复
        QueryWrapper<ReimMain> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("reim_status", 1)
                    .le("update_time", fiveMinsAgo);
                    
        List<ReimMain> pendingList = reimMainService.list(queryWrapper);
        
        for (ReimMain main : pendingList) {
            String reimId = main.getId();
            String pushedKey = "fk:reim:pushed:" + reimId;
            String retryKey = "fk:reim:retry:" + reimId;
            
            // 检查是否已经推送成功
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(pushedKey))) {
                continue;
            }
            
            // 获取重试次数
            String retryStr = stringRedisTemplate.opsForValue().get(retryKey);
            int retryCount = retryStr == null ? 0 : Integer.parseInt(retryStr);
            
            if (retryCount >= 3) {
                log.error("报警：报销单 {} 推送BPM超3次失败，请人工介入处理！", reimId);
                continue;
            }
            
            try {
                // 重新推送
                log.info("补偿推送报销单 {} 到 MQ", reimId);
                rabbitTemplate.convertAndSend("reim.submit.topic", "", reimId);
                
                // 推送成功后记录到 Redis，假设有效期30天
                stringRedisTemplate.opsForValue().set(pushedKey, "1", 30, java.util.concurrent.TimeUnit.DAYS);
            } catch (Exception e) {
                log.error("补偿推送报销单 {} 失败", reimId, e);
                // 重试次数+1
                stringRedisTemplate.opsForValue().increment(retryKey);
            }
        }
        
        log.info("BPM审批流推送补偿任务执行结束。");
    }
}
