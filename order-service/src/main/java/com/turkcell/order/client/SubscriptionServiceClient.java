package com.turkcell.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "subscription-service")
public interface SubscriptionServiceClient {

    @PostMapping("/api/v1/subscriptions")
    SubscriptionClientResponse createSubscription(@RequestBody SubscriptionClientRequest request);
}
