package com.iot.mops.controller.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    public TopicExchange iotExchange(@Value("${app.rabbit.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }
}
