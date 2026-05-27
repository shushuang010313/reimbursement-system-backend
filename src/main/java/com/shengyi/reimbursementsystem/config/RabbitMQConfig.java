package com.shengyi.reimbursementsystem.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String REIM_SUBMIT_TOPIC = "reim.submit.topic";
    public static final String REIM_APPROVE_TOPIC = "reim.approve.topic";
    
    // 队列名称
    public static final String REIM_SUBMIT_QUEUE = "reim.submit.queue";
    
    // 死信队列与交换机
    public static final String REIM_DLX_EXCHANGE = "reim.dlx.exchange";
    public static final String REIM_DLX_QUEUE = "reim.dlx.queue";
    public static final String REIM_DLX_ROUTING_KEY = "reim.dlx.routing.key";

    // ================== 正常交换机与队列 ==================
    @Bean
    public TopicExchange submitExchange() {
        return ExchangeBuilder.topicExchange(REIM_SUBMIT_TOPIC).durable(true).build();
    }

    @Bean
    public TopicExchange approveExchange() {
        return ExchangeBuilder.topicExchange(REIM_APPROVE_TOPIC).durable(true).build();
    }

    @Bean
    public org.springframework.amqp.core.Queue submitQueue() {
        // 配置该队列的死信交换机和死信路由键
        return org.springframework.amqp.core.QueueBuilder.durable(REIM_SUBMIT_QUEUE)
                .withArgument("x-dead-letter-exchange", REIM_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", REIM_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public org.springframework.amqp.core.Binding submitBinding() {
        // 绑定 submitQueue 到 submitExchange，路由键为 "#"（接收所有消息）
        return org.springframework.amqp.core.BindingBuilder.bind(submitQueue()).to(submitExchange()).with("#");
    }

    // ================== 死信交换机与队列 ==================
    @Bean
    public org.springframework.amqp.core.DirectExchange dlxExchange() {
        return org.springframework.amqp.core.ExchangeBuilder.directExchange(REIM_DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public org.springframework.amqp.core.Queue dlxQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(REIM_DLX_QUEUE).build();
    }

    @Bean
    public org.springframework.amqp.core.Binding dlxBinding() {
        return org.springframework.amqp.core.BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with(REIM_DLX_ROUTING_KEY);
    }
}
