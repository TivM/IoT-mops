package com.iot.mops.ruleengine.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
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

    @Bean
    public Queue ruleQueue(@Value("${app.rabbit.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding ruleBinding(Queue ruleQueue,
                               TopicExchange iotExchange,
                               @Value("${app.rabbit.routing-key}") String routingKey) {
        return BindingBuilder.bind(ruleQueue).to(iotExchange).with(routingKey);
    }
}
