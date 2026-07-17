package com.turkcell.subscription.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * FR-08: PaymentCompleted tuketildiginde, aboneligin hangi tarife VERSIYONUNA baglandigini
 * pinlemek icin guncel versiyon numarasi senkron cekilir (Bolum 8.1 "fiyat/veri snapshot'i
 * senkron olmali" ile ayni gerekce).
 */
@FeignClient(name = "product-catalog-service")
public interface ProductCatalogServiceClient {

    @GetMapping("/api/v1/tariffs/{code}")
    TariffClientDto getTariff(@PathVariable("code") String code);
}
