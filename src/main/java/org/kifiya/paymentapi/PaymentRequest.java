package org.kifiya.paymentapi;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRequest {
    @NotBlank
    private String orderId;
    @NotNull
    private AmountDto amount;
    @NotNull
    private PaymentMethodDto paymentMethod;
    @NotBlank
    private String customerId;
    @NotBlank
    private String description;
    @NotBlank
    private String callbackUrl;
    @NotBlank
    private String idempotencyKey;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AmountDto {
        private String value;
        private String currency;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentMethodDto {
        private String type;
        private String cardNumber;
        private String expirationMonth;
        private String expirationYear;
        private String cvv;
        private String cardHolderName;
    }
}