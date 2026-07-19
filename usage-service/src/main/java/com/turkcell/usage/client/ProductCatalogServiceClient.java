package com.turkcell.usage.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-catalog-service")
public interface ProductCatalogServiceClient {

    @GetMapping("/api/v1/tariffs/{code}")
    TariffClientDto getTariff(@PathVariable("code") String code);
}
