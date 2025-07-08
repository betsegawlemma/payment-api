package org.kifiya.paymentapi.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.kifiya.paymentapi.model.OutboxEvent;
import org.kifiya.paymentapi.model.PaymentOrder;
import org.kifiya.paymentapi.model.PaymentOutboxPayload;
import org.kifiya.paymentapi.model.PaymentStatus;
import org.kifiya.paymentapi.repository.OutboxEventRepository;
import org.kifiya.paymentapi.repository.PaymentOrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PaymentOutboxHelper {

    private final PaymentOrderRepository paymentRepo;
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    /**
     * Each update+outbox runs in its own REQUIRES_NEW transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAndOutbox(
            PaymentOrder order,
            PaymentStatus status,
            String message
    ) {
        order.setStatus(status);
        order.setProviderMessage(message);
        order.setUpdatedAt(Instant.now());
        paymentRepo.save(order);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(
                    new PaymentOutboxPayload(order.getOrderId(), status.name())
            );
        } catch (JsonProcessingException e) {
            // fallback to minimal manual payload
            payloadJson = "{\"orderId\":\"" + order.getOrderId() +
                    "\",\"status\":\"" + status.name() + "\"}";
        }

        outboxRepo.save(OutboxEvent.builder()
                .eventType("PAYMENT_" + status.name())
                .payload(payloadJson)
                .sent(false)
                .createdAt(Instant.now())
                .build()
        );
    }
}
