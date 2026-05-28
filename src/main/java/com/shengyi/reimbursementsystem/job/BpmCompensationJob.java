package com.shengyi.reimbursementsystem.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shengyi.reimbursementsystem.component.DingTalkNotifier;
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
import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class BpmCompensationJob {

    private final IReimMainService reimMainService;
    private final DingTalkNotifier dingtalkNotifier;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 定时任务：扫描状态为已完成（待推送）且超5分钟未处理的数据
     * 每5分钟执行一次
     * 【学习指引】补偿定时任务：用于解决“本地消息已落库，但发送 MQ 失败”，或者“服务意外宕机”导致的断点问题，保障最终一致性。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void compensateBpmPush() {
        log.info("开始执行BPM审批流推送补偿任务...");
        dingtalkNotifier.send("开始执行BPM审批流推送补偿任务...");
        
        // 【学习指引】1. 锁定“异常”时间窗口：为了避开正常处理中的数据，只扫描 5 分钟前更新的数据
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
            
            // 【学习指引】2. 幂等性校验：通过 Redis 检查该单据是否已被补偿推送成功，避免重复推送
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(pushedKey))) {
                continue;
            }
            
            // 【学习指引】3. 熔断机制：限制最大补偿重试次数（例如3次），防止死循环和无效消耗
            String retryStr = stringRedisTemplate.opsForValue().get(retryKey);
            int retryCount = retryStr == null ? 0 : Integer.parseInt(retryStr);
            
            if (retryCount >= 3) {
                log.error("报警：报销单 {} 推送BPM超3次失败，请人工介入处理！", reimId);
                dingtalkNotifier.send("报警：报销单 " + reimId + " 推送BPM超3次失败，请人工介入处理！");
                continue;
            }
            
            try {
                // 【学习指引】4. 重新推送消息到 MQ
                log.info("补偿推送报销单 {} 到 MQ", reimId);
                dingtalkNotifier.send("补偿推送报销单 " + reimId + " 到 MQ");
                rabbitTemplate.convertAndSend("reim.submit.topic", "", reimId);
                
                // 【学习指引】推送成功后记录标志到 Redis（可以设置过期时间，这里设为30天）
                stringRedisTemplate.opsForValue().set(pushedKey, "1", 30, TimeUnit.DAYS);
            } catch (Exception e) {
                log.error("补偿推送报销单 {} 失败", reimId, e);
                dingtalkNotifier.send("补偿推送报销单 " + reimId + " 失败");
                // 【学习指引】推送失败则累加重试次数
                stringRedisTemplate.opsForValue().increment(retryKey);
            }
        }
        
        log.info("BPM审批流推送补偿任务执行结束。");
        dingtalkNotifier.send("BPM审批流推送补偿任务执行结束。");
    }
}
