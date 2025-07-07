package org.kifiya.paymentapi;

import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class PaymentMethod {
    private String type;
    private String cardNumber;
    private String expirationMonth;
    private String expirationYear;
    private String cvv;
    private String cardHolderName;
}