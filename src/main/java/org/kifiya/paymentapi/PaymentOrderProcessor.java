package org.kifiya.paymentapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrderProcessor {

    private final PaymentOrderRepository paymentRepo;
    private final TokenBucketRateLimiter rateLimiter;
    private final ProviderAdapter providerAdapter;
    private final PaymentOutboxHelper outboxHelper;

    private static final int MAX_RETRIES = 5;
    private static final long BASE_DELAY_MS = 1_000L;

    @Transactional
    public void processOrder(Long paymentId) {
        Optional<PaymentOrder> optOrder = paymentRepo.findById(paymentId);
        if (optOrder.isEmpty()) {
            log.warn("PaymentOrder {} not found", paymentId);
            return;
        }

        PaymentOrder order = optOrder.get();
        if (order.getStatus() != PaymentStatus.PENDING) {
            log.info("Order {} already {}", order.getOrderId(), order.getStatus());
            return;
        }

        PaymentResponse resp = attemptPayment(order);
        ProviderStatus st = ProviderStatus.from(resp.getStatus());

        if (st == ProviderStatus.SUCCESS) {
            outboxHelper.updateAndOutbox(order, PaymentStatus.COMPLETED, resp.getMessage());
        } else if (st.isRetryable()) {
            retryExponential(order);
        } else {
            outboxHelper.updateAndOutbox(order, PaymentStatus.FAILED, resp.getMessage());
        }
    }

    private void retryExponential(PaymentOrder order) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            sleepBackoff(attempt);

            PaymentResponse resp = attemptPayment(order);
            ProviderStatus st = ProviderStatus.from(resp.getStatus());

            if (st == ProviderStatus.SUCCESS) {
                outboxHelper.updateAndOutbox(order, PaymentStatus.COMPLETED, resp.getMessage());
                return;
            }
            if (!st.isRetryable()) {
                outboxHelper.updateAndOutbox(order, PaymentStatus.FAILED, resp.getMessage());
                return;
            }
        }
        outboxHelper.updateAndOutbox(order, PaymentStatus.FAILED, "Max retries exceeded");
    }

    private PaymentResponse attemptPayment(PaymentOrder order) {
        boolean permit = rateLimiter.tryAcquire();
        while (!permit) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            permit = rateLimiter.tryAcquire();
        }
        return providerAdapter.processPayment(order);
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(BASE_DELAY_MS * (1L << attempt));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
