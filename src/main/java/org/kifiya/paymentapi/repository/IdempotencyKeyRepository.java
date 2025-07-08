package org.kifiya.paymentapi.repository;

import org.kifiya.paymentapi.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByIdemKey(String idemKey);
}
