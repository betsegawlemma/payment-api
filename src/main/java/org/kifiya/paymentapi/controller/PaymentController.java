package org.kifiya.paymentapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.kifiya.paymentapi.dto.PaymentRequest;
import org.kifiya.paymentapi.dto.PaymentResponse;
import org.kifiya.paymentapi.model.*;
import org.kifiya.paymentapi.repository.IdempotencyKeyRepository;
import org.kifiya.paymentapi.repository.PaymentOrderRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentOrderRepository paymentRepo;
    private final IdempotencyKeyRepository idempRepo;
    private final RabbitTemplate rabbitTemplate;

    @Value("${payments.queue}")
    private String queueName;

    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestHeader("Idempotency-Key") String idemKey,
            @Valid @RequestBody PaymentRequest req
    ) {
        Optional<IdempotencyKey> existing = idempRepo.findByIdemKey(idemKey);
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new PaymentResponse(req.getOrderId(), "CONFLICT", "Duplicate idempotency key"));
        }

        PaymentOrder order = PaymentOrder.builder()
                .orderId(req.getOrderId())
                .amount(new Amount(req.getAmount().getValue(), req.getAmount().getCurrency()))
                .paymentMethod(new PaymentMethod(
                        req.getPaymentMethod().getType(),
                        req.getPaymentMethod().getCardNumber(),
                        req.getPaymentMethod().getExpirationMonth(),
                        req.getPaymentMethod().getExpirationYear(),
                        req.getPaymentMethod().getCvv(),
                        req.getPaymentMethod().getCardHolderName()
                ))
                .customerId(req.getCustomerId())
                .description(req.getDescription())
                .callbackUrl(req.getCallbackUrl())
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PaymentOrder savedOrder = paymentRepo.save(order);
        idempRepo.save(new IdempotencyKey(null, idemKey, savedOrder.getId()));

        // Enqueue for worker processing
        rabbitTemplate.convertAndSend(queueName, savedOrder.getId());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new PaymentResponse(req.getOrderId(), "PENDING", "Payment queued for processing."));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String orderId) {
        Optional<PaymentOrder> opt = paymentRepo.findByOrderId(orderId);
        if (opt.isPresent()) {
            PaymentOrder order = opt.get();
            return ResponseEntity.ok(
                    new PaymentResponse(order.getOrderId(), order.getStatus().name(), order.getProviderMessage())
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PaymentResponse(orderId, "NOT_FOUND", "Order not found"));
        }
    }
}