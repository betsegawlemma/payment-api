package org.kifiya.paymentapi.util;

import lombok.RequiredArgsConstructor;
import org.kifiya.paymentapi.dto.PaymentResponse;
import org.kifiya.paymentapi.model.PaymentOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ExternalPaymentProviderAdapter implements ProviderAdapter {

    @Value("${payments.provider.url}")
    private String providerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public PaymentResponse processPayment(PaymentOrder order) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PaymentOrder> entity = new HttpEntity<>(order, headers);
        try {
            ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(providerUrl, entity, PaymentResponse.class);
            return response.getBody();
        } catch (Exception e) {
            PaymentResponse error = new PaymentResponse();
            error.setOrderId(order.getOrderId());
            error.setStatus("ERROR");
            error.setMessage("Provider call failed: " + e.getMessage());
            return error;
        }
    }
}