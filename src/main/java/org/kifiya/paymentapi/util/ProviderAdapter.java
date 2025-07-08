package org.kifiya.paymentapi.util;

import org.kifiya.paymentapi.dto.PaymentResponse;
import org.kifiya.paymentapi.model.PaymentOrder;

public interface ProviderAdapter {
    PaymentResponse processPayment(PaymentOrder order);
}