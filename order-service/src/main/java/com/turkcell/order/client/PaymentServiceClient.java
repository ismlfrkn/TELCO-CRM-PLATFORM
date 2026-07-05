package com.turkcell.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments")
    PaymentClientResponse createPayment(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                         @RequestBody PaymentClientRequest request);

    @PostMapping("/api/v1/payments/{id}/refund")
    PaymentClientResponse refund(@PathVariable("id") UUID id);
}
