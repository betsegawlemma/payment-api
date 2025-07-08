package org.kifiya.paymentapi.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "idempotency_keys", uniqueConstraints = @UniqueConstraint(columnNames = "idemKey"))
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String idemKey;
    private Long paymentId;
}