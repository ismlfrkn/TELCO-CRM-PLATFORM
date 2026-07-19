package com.turkcell.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-catalog-service")
public interface ProductCatalogServiceClient {

    @GetMapping("/api/v1/tariffs/{code}")
    TariffClientDto getTariff(@PathVariable("code") String code);

    @GetMapping("/api/v1/tariffs/{code}/versions/{version}")
    TariffVersionClientDto getTariffVersion(@PathVariable("code") String code, @PathVariable("version") int version);
}
