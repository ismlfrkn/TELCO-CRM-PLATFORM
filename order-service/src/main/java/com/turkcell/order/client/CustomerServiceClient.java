package com.turkcell.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "customer-service")
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{id}")
    CustomerClientDto getCustomer(@PathVariable("id") UUID id);
}
