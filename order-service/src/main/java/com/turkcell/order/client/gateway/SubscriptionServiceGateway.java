package com.turkcell.order.client.gateway;

import com.turkcell.order.client.SubscriptionClientRequest;
import com.turkcell.order.client.SubscriptionClientResponse;
import com.turkcell.order.client.SubscriptionServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionServiceGateway {

    private final SubscriptionServiceClient subscriptionServiceClient;

    public SubscriptionServiceGateway(SubscriptionServiceClient subscriptionServiceClient) {
        this.subscriptionServiceClient = subscriptionServiceClient;
    }

    // createSubscription icin idempotency key altyapisi yok (her cagri yeni MSISDN tahsis edip
    // yeni bir kayit olusturur) - bu yuzden retry EKLENMEDI (timeout sonrasi tekrar denemek
    // mukerrer abonelik/MSISDN tahsisi riski tasir), sadece circuit breaker korumasi var.
    @CircuitBreaker(name = "subscription-service")
    public SubscriptionClientResponse createSubscription(SubscriptionClientRequest request) {
        return subscriptionServiceClient.createSubscription(request);
    }
}
