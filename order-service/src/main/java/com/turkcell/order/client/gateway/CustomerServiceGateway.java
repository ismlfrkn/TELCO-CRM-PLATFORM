package com.turkcell.order.client.gateway;

import com.turkcell.order.client.CustomerClientDto;
import com.turkcell.order.client.CustomerServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
