package com.shengyi.reimbursementsystem.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DingTalkNotifier {

    @Value("${dingtalk.webhook}")
    private String dingtalkWebhook;

    private final RestTemplate restTemplate = new RestTemplate();

    public void send(String message) {
        try {
            if (dingtalkWebhook == null || dingtalkWebhook.isEmpty()) {
                return;
            }
            // 注意：钉钉自定义机器人通常有安全设置，例如关键字必须包含“告警”等
            // 为确保所有消息都能发出去，如果消息没带关键字，可以在前面统一加上。
            // 这里我们假设所有关于BPM的消息都加个前缀。
            String content = "【BPM 消息】" + message;
            
            Map<String, Object> textMap = new HashMap<>();
            textMap.put("content", content);

            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("msgtype", "text");
            reqMap.put("text", textMap);

            restTemplate.postForObject(dingtalkWebhook, reqMap, String.class);
            log.info("【钉钉推送成功】");
        } catch (Exception e) {
            log.error("【钉钉推送失败】: ", e);
        }
    }
}
