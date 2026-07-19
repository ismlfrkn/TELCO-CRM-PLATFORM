package com.turkcell.order.client.gateway;

import com.turkcell.order.client.CustomerClientDto;
import com.turkcell.order.client.CustomerServiceClient;
import com.turkcell.order.client.ProductCatalogServiceClient;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class GatewayResilienceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("order_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    private CustomerServiceClient customerServiceClient;

    @MockBean
    private ProductCatalogServiceClient productCatalogServiceClient;

    @Autowired
    private CustomerServiceGateway customerServiceGateway;

    @Autowired
    private ProductCatalogServiceGateway productCatalogServiceGateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @AfterEach
    void resetCircuitBreakers() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> cb.reset());
    }

    @Test
    void getCustomer_whenTransientFailuresThenSuccess_retriesAndEventuallySucceeds() {
        CustomerClientDto customer = new CustomerClientDto();
        customer.setStatus("ACTIVE");
        when(customerServiceClient.getCustomer(any()))
                .thenThrow(serviceUnavailable())
                .thenThrow(serviceUnavailable())
                .thenReturn(customer);

        CustomerClientDto result = customerServiceGateway.getCustomer(UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(customerServiceClient, times(3)).getCustomer(any());
    }

    @Test
    void getTariff_whenNotFound_doesNotRetryAndPropagatesImmediately() {
        when(productCatalogServiceClient.getTariff("MISSING")).thenThrow(notFound());

        assertThatThrownBy(() -> productCatalogServiceGateway.getTariff("MISSING"))
                .isInstanceOf(FeignException.NotFound.class);

        verify(productCatalogServiceClient, times(1)).getTariff("MISSING");
    }

    private FeignException.NotFound notFound() {
        return (FeignException.NotFound) FeignException.errorStatus("Client#method()", response(404));
    }

    private FeignException.ServiceUnavailable serviceUnavailable() {
        return (FeignException.ServiceUnavailable) FeignException.errorStatus("Client#method()", response(503));
    }

    private Response response(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/resource",
                Collections.emptyMap(), null, StandardCharsets.UTF_8);
        return Response.builder()
                .status(status)
                .request(request)
                .headers(Collections.emptyMap())
                .build();
    }
}
