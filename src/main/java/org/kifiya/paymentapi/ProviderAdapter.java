package org.kifiya.paymentapi;

public interface ProviderAdapter {
    PaymentResponse processPayment(PaymentOrder order);
}