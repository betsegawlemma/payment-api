package org.kifiya.paymentapi;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentOutboxPayload {
    private String orderId;
    private String status;
}