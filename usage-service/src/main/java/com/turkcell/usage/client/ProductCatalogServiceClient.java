package com.turkcell.usage.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * SubscriptionActivated tuketildiginde Quota acmak icin tarifenin minutes/sms/mb dahil degerleri
 * senkron cekilir (Bolum 8.1 "fiyat/veri snapshot'i senkron olmali" ile ayni gerekce).
 */
@FeignClient(name = "product-catalog-service")
public interface ProductCatalogServiceClient {

    @GetMapping("/api/v1/tariffs/{code}")
    TariffClientDto getTariff(@PathVariable("code") String code);
}
