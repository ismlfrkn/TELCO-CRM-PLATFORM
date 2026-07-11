package com.turkcell.order.client.gateway;

import com.turkcell.order.client.AddonClientDto;
import com.turkcell.order.client.ProductCatalogServiceClient;
import com.turkcell.order.client.TariffClientDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

/**
 * bkz. CustomerServiceGateway - ayni gerekce: fiyat sorgusu GET/idempotent oldugu icin retry guvenli,
 * FeignException.NotFound (urun yok) resilience4j'in izledigi exception listelerine dahil degil.
 */
@Component
public class ProductCatalogServiceGateway {

    private final ProductCatalogServiceClient productCatalogServiceClient;

    public ProductCatalogServiceGateway(ProductCatalogServiceClient productCatalogServiceClient) {
        this.productCatalogServiceClient = productCatalogServiceClient;
    }

    @CircuitBreaker(name = "product-catalog-service")
    @Retry(name = "product-catalog-service")
    public TariffClientDto getTariff(String code) {
        return productCatalogServiceClient.getTariff(code);
    }

    @CircuitBreaker(name = "product-catalog-service")
    @Retry(name = "product-catalog-service")
    public AddonClientDto getAddon(String code) {
        return productCatalogServiceClient.getAddon(code);
    }
}
