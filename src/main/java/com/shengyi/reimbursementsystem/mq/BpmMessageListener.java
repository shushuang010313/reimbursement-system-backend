package com.shengyi.reimbursementsystem.mq;

import com.shengyi.reimbursementsystem.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 审批流(BPM)消息监听器
 * 用于演示分布式场景下的最终一致性保障：自动重试与死信队列机制
 */
@Slf4j
@Component
public class BpmMessageListener {

    /**
     * 正常消费提交报销单的消息
     * 监听队列：reim.submit.queue
     *
     * @param reimId 报销单ID
     */
    @RabbitListener(queues = RabbitMQConfig.REIM_SUBMIT_QUEUE)
    public void handleSubmitMessage(String reimId) {
        log.info("【BPM消费者】接收到报销单提交消息，准备推送审批流，报销单ID: {}", reimId);

        // 为了演示 Highlight 3：分布式容错与重试机制，故意模拟网络调用异常
        log.warn("【BPM消费者】调用BPM系统接口发生网络超时异常！(演示用途)");
        throw new RuntimeException("调用BPM审批流网络超时！");
        
        // 实际业务代码应类似于：
        // boolean success = bpmClient.submit(reimId);
        // if (!success) {
        //     throw new BpmPushException("向审批流推送失败");
        // }
        // log.info("【BPM消费者】报销单 {} 已成功推送到审批流", reimId);
    }

    /**
     * 死信队列消费者（人工干预兜底）
     * 监听队列：reim.dlx.queue
     *
     * @param reimId 报销单ID
     */
    @RabbitListener(queues = RabbitMQConfig.REIM_DLX_QUEUE)
    public void handleDeadLetterMessage(String reimId) {
        // 当达到最大重试次数（3次）依然失败时，消息将被投递到此
        log.error("=========================================================");
        log.error("【严重告警】向BPM推送报销单(ID: {})失败，重试3次已耗尽！", reimId);
        log.error("【严重告警】该消息已进入死信队列(DLQ)，系统已触发钉钉/邮件告警，请管理员立即人工介入处理！");
        log.error("=========================================================");
        
        // 实际业务中这里可以调用告警接口（钉钉机器人、发送邮件等），并将失败记录持久化到告警表
    }
}
