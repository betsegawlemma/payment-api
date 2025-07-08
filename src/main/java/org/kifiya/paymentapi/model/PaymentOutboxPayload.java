package org.kifiya.paymentapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentOutboxPayload {
    private String orderId;
    private String status;
}