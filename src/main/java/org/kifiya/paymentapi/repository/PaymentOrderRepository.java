package org.kifiya.paymentapi.repository;

import org.kifiya.paymentapi.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderId(String orderId);
}