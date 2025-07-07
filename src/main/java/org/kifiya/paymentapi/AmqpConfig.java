package org.kifiya.paymentapi;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfig {

    @Value("${payments.queue}")
    private String queueName;
    @Value("${payments.events-exchange}")
    private String exchangeName;

    @Bean
    public Queue paymentsQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Exchange eventsExchange() {
        return ExchangeBuilder.fanoutExchange(exchangeName).durable(true).build();
    }

    @Bean
    public Binding binding(Queue paymentsQueue, Exchange eventsExchange) {
        return BindingBuilder.bind(paymentsQueue).to(eventsExchange).with("").noargs();
    }
}