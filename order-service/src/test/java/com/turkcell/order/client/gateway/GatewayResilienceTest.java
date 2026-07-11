package com.turkcell.order.client.gateway;

import com.turkcell.order.client.CustomerClientDto;
import com.turkcell.order.client.CustomerServiceClient;
import com.turkcell.order.client.ProductCatalogServiceClient;
import com.turkcell.order.client.SubscriptionClientRequest;
import com.turkcell.order.client.SubscriptionServiceClient;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

/**
 * order-service saga'sinin en kirilgan noktasi olan Feign zincirine (Bolum 9.2) eklenen circuit
 * breaker + retry wiring'inin GERCEKTEN devrede oldugunu dogrular - resilience4j kutuphanesinin
 * kendi mantigini degil, bu projedeki configs/order-service/application.yml + @CircuitBreaker/@Retry
 * kablolamasinin dogrulugunu test eder. subscription-service kasitli olarak retry'siz (sadece CB)
 * oldugu icin devre acilma senaryosunu retry etkisi olmadan izole sekilde test etmeye elverisli.
 */
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

    @MockitoBean
    private CustomerServiceClient customerServiceClient;

    @MockitoBean
    private ProductCatalogServiceClient productCatalogServiceClient;

    @MockitoBean
    private SubscriptionServiceClient subscriptionServiceClient;

    @Autowired
    private CustomerServiceGateway customerServiceGateway;

    @Autowired
    private ProductCatalogServiceGateway productCatalogServiceGateway;

    @Autowired
    private SubscriptionServiceGateway subscriptionServiceGateway;

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

        // FeignException.NotFound retryExceptions/recordExceptions listelerinde olmadigi icin
        // tek bir denemeyle dogrudan cagirana ulasmali - retry donguleri tetiklenmemeli.
        verify(productCatalogServiceClient, times(1)).getTariff("MISSING");
    }

    @Test
    void createSubscription_whenDownstreamRepeatedlyFails_circuitOpensAndShortCircuitsWithoutCallingClient() {
        when(subscriptionServiceClient.createSubscription(any())).thenThrow(serviceUnavailable());

        // minimumNumberOfCalls=5 (configs/order-service/application.yml default) - bu kadar
        // basarisiz cagridan sonra failure rate degerlendirilip devre acilir.
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> subscriptionServiceGateway.createSubscription(subscriptionRequest()))
                    .isInstanceOf(FeignException.class);
        }

        assertThatThrownBy(() -> subscriptionServiceGateway.createSubscription(subscriptionRequest()))
                .isInstanceOf(CallNotPermittedException.class);

        // 6. cagri devre acik oldugu icin gercek client'a hic ulasmadi (fail-fast, retry yok).
        verify(subscriptionServiceClient, times(5)).createSubscription(any());
    }

    private SubscriptionClientRequest subscriptionRequest() {
        SubscriptionClientRequest request = new SubscriptionClientRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setTariffCode("TARIFF100");
        return request;
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
