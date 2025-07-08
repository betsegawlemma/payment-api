package org.kifiya.paymentapi.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "outbox")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType; // e.g. "PAYMENT_COMPLETED", "PAYMENT_FAILED"
    @Lob
    private String payload;
    private boolean sent;
    private Instant createdAt;
}