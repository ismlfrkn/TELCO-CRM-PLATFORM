package com.turkcell.order.client.gateway;

import com.turkcell.order.client.CustomerClientDto;
import com.turkcell.order.client.CustomerServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Saga'nin en kirilgan noktasi olan senkron Feign zincirini (Bolum 9.2) circuit breaker + retry ile
 * korur. FeignException.NotFound (musteri yok) transient bir altyapi hatasi degil normal bir is
 * sonucu oldugu icin resilience4j config'indeki recordExceptions/retryExceptions listelerine dahil
 * edilmedi - retry/CB tetiklenmeden dogrudan cagirana ulasir (bkz. configs/order-service).
 */
@Component
public class CustomerServiceGateway {

    private final CustomerServiceClient customerServiceClient;

    public CustomerServiceGateway(CustomerServiceClient customerServiceClient) {
        this.customerServiceClient = customerServiceClient;
    }

    @CircuitBreaker(name = "customer-service")
    @Retry(name = "customer-service")
    public CustomerClientDto getCustomer(UUID id) {
        return customerServiceClient.getCustomer(id);
    }
}
