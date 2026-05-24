package com.shengyi.reimbursementsystem.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String REIM_SUBMIT_TOPIC = "reim.submit.topic";
    public static final String REIM_APPROVE_TOPIC = "reim.approve.topic";

    @Bean
    public TopicExchange submitExchange() {
        return ExchangeBuilder.topicExchange(REIM_SUBMIT_TOPIC).durable(true).build();
    }

    @Bean
    public TopicExchange approveExchange() {
        return ExchangeBuilder.topicExchange(REIM_APPROVE_TOPIC).durable(true).build();
    }

}
