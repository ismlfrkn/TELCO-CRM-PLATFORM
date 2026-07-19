package com.turkcell.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "subscription-service")
public interface SubscriptionServiceClient {

    @GetMapping("/api/v1/subscriptions/active")
    PageResponse<SubscriptionClientDto> getActiveSubscriptions(Pageable pageable);
}
