package com.shengyi.reimbursementsystem.mq;

import com.shengyi.reimbursementsystem.config.RabbitMQConfig;
import com.shengyi.reimbursementsystem.component.DingTalkNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 审批流(BPM)消息监听器
 * 用于演示分布式场景下的最终一致性保障：自动重试与死信队列机制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BpmMessageListener {

    private final DingTalkNotifier dingtalkNotifier;

    /**
     * 正常消费提交报销单的消息
     * 监听队列：reim.submit.queue
     * 【答辩重点】为什么用 MQ 做最终一致性？
     * 答：报销系统是核心内部系统，BPM审批流是外部系统。如果网络断连，我们不能让财务人员提交单据失败。
     * 引入 MQ 解耦后，主单流转成功就立刻返回给前端“提交成功”，后续利用 MQ 的可靠投递通知 BPM，哪怕 BPM 宕机，也不会影响内部单据的保存。
     *
     * @param reimId 报销单ID
     */
    @RabbitListener(queues = RabbitMQConfig.REIM_SUBMIT_QUEUE)
    public void handleSubmitMessage(String reimId) {
        log.info("【BPM消费者】接收到报销单提交消息，准备推送审批流，报销单ID: {}", reimId);
        dingtalkNotifier.send("接收到报销单提交消息，准备推送审批流，报销单ID: " + reimId);

        // 为了演示 Highlight 3：分布式容错与重试机制，故意模拟网络调用异常
        // 【答辩重点】失败自动指数退避重试机制
        // 如果这里抛出异常（网络断连/超时），Spring AMQP 会根据配置文件进行重试。
        // 配置了诸如 multiplier=2, maxAttempts=3 的参数，实现指数级退避（比如隔1秒、2秒、4秒再试），避免发生雪崩压垮外部系统。
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
     * 【答辩重点】什么是死信队列 (DLX) 触发人工兜底？
     * 答：如果 BPM 系统彻底挂了，重试 3 次依然失败，这条消息就会被 RabbitMQ 自动路由到“死信交换机 (DLX)”，然后进入本队列。
     * 这是 100% 不丢单的最后一道防线！在这里触发最高级别的钉钉/邮件告警，由开发人员和运维介入进行人工补偿。
     *
     * @param reimId 报销单ID
     */
    @RabbitListener(queues = RabbitMQConfig.REIM_DLX_QUEUE)
    public void handleDeadLetterMessage(String reimId) {
        // 当达到最大重试次数（3次）依然失败时，消息将被投递到此
        // 【学习指引】作为最终兜底方案，在死信队列中通常会触发严重告警，并要求人工介入排查
        log.error("=========================================================");
        log.error("【严重告警】向BPM推送报销单(ID: {})失败，重试3次已耗尽！", reimId);
        log.error("【严重告警】该消息已进入死信队列(DLQ)，系统已触发钉钉/邮件告警，请管理员立即人工介入处理！");
        log.error("=========================================================");
        
        // 实际业务中这里可以调用告警接口（钉钉机器人、发送邮件等），并将失败记录持久化到告警表
        dingtalkNotifier.send("【严重告警】报销单(ID: " + reimId + ") 推送BPM失败，重试3次已耗尽，请立即人工介入！");
    }
}
