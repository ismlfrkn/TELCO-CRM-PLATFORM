package com.turkcell.order.client.gateway;

import com.turkcell.order.client.PaymentClientRequest;
import com.turkcell.order.client.PaymentClientResponse;
import com.turkcell.order.client.PaymentServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentServiceGateway {

    private final PaymentServiceClient paymentServiceClient;

    public PaymentServiceGateway(PaymentServiceClient paymentServiceClient) {
        this.paymentServiceClient = paymentServiceClient;
    }

    // createPayment Idempotency-Key ile korunuyor (FR-26) - ayni istek tekrar denendiginde
    // payment-service ayni sonucu dondurur, bu yuzden retry burada guvenli.
    @CircuitBreaker(name = "payment-service")
    @Retry(name = "payment-service")
    public PaymentClientResponse createPayment(String idempotencyKey, PaymentClientRequest request) {
        return paymentServiceClient.createPayment(idempotencyKey, request);
    }

    // refund'un idempotency key'i yok (sadece durum bazli koruma var: yalnizca COMPLETED odemeler
    // iade edilebilir) - kompanzasyon akisinda yanlislikla tekrar tetiklenmesini onlemek icin retry
    // eklenmedi, sadece circuit breaker korumasi var.
    @CircuitBreaker(name = "payment-service")
    public PaymentClientResponse refund(UUID paymentId) {
        return paymentServiceClient.refund(paymentId);
    }
}
