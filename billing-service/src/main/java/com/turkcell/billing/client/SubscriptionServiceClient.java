package com.turkcell.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * FR-21: otomatik bill-run icin - "hangi aboneler faturalanacak" bilgisi artik cagirandan elle
 * alinmiyor, subscription-service'in /subscriptions/active endpoint'inden senkron cekiliyor
 * (ProductCatalogServiceClient ile ayni gerekce - Bolum 8.1 "fiyat/veri snapshot'i" senkron olmali).
 */
@FeignClient(name = "subscription-service")
public interface SubscriptionServiceClient {

    @GetMapping("/api/v1/subscriptions/active")
    PageResponse<SubscriptionClientDto> getActiveSubscriptions(Pageable pageable);
}
