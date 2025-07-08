package org.kifiya.paymentapi.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Amount {
    private String value;
    private String currency;
}