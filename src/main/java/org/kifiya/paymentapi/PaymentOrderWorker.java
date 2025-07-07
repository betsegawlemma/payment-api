package org.kifiya.paymentapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentOrderWorker {

    private final TaskExecutor paymentTaskExecutor;

    private final PaymentOrderProcessor processor;

    @RabbitListener(queues = "${payments.queue}")
    public void handlePaymentOrder(Long paymentId) {
        paymentTaskExecutor.execute(() ->
                processor.processOrder(paymentId)
        );
    }
}
