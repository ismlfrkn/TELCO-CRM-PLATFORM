package com.turkcell.payment.service;

import com.turkcell.payment.dto.request.PaymentCreateRequest;
import com.turkcell.payment.dto.response.PaymentResponse;
import com.turkcell.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class PaymentIdempotencyConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("payment_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void createPayment_concurrentRequestsWithSameIdempotencyKey_onlyCreatesOnePayment() throws Exception {
        String idempotencyKey = "concurrent-key-" + UUID.randomUUID();
        int concurrentRequests = 15;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch allThreadsReady = new CountDownLatch(concurrentRequests);
        CountDownLatch startSignal = new CountDownLatch(1);

        List<Future<PaymentResponse>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executor.submit(() -> {
                allThreadsReady.countDown();
                startSignal.await();
                PaymentCreateRequest request = new PaymentCreateRequest();
                request.setInvoiceId(UUID.randomUUID());
                request.setAmount(new BigDecimal("42.00"));
                request.setMethod("CARD");
                return paymentService.createPayment(idempotencyKey, request);
            }));
        }

        allThreadsReady.await(5, TimeUnit.SECONDS);
        startSignal.countDown();

        Set<UUID> resultingPaymentIds = new HashSet<>();
        for (Future<PaymentResponse> future : futures) {
            resultingPaymentIds.add(future.get(10, TimeUnit.SECONDS).getId());
        }
        executor.shutdown();

        assertThat(resultingPaymentIds).hasSize(1);
        assertThat(paymentRepository.count()).isEqualTo(1);
    }
}
