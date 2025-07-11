package org.kifiya.paymentapi.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {
    private String orderId;
    private String status;
    private String message;
}