package org.kifiya.paymentapi.util;

import lombok.RequiredArgsConstructor;
import org.kifiya.paymentapi.model.OutboxEvent;
import org.kifiya.paymentapi.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxRepo;
    private final RabbitTemplate rabbitTemplate;

    @Value("${payments.events-exchange}")
    private String eventsExchange;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxRepo.findBySentFalse();
        for (OutboxEvent event : events) {
            rabbitTemplate.convertAndSend(eventsExchange, "", event.getPayload());
            event.setSent(true);
            outboxRepo.save(event);
        }
    }
}