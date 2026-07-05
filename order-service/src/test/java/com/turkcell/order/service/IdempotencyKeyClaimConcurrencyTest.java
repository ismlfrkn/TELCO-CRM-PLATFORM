package com.turkcell.order.service;

import com.turkcell.order.exception.OrderProcessingInProgressException;
import com.turkcell.order.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * payment-service (Idempotency-Key), usage-service (externalCdrId) ve billing-service
 * (subscriptionId+periyot) ile ayni temel eszamanlilik dersinin order-service'teki dorduncu
 * uygulamasi, ama burada "claim" deseni test ediliyor: saga BASLAMADAN once anahtar rezerve edilir,
 * bu yuzden ayni Idempotency-Key ile ayni anda gelen isteklerden SADECE BIRI claim'i kazanmali
 * (bos Optional donmeli), digerleri saga hala islenmekte oldugu icin
 * OrderProcessingInProgressException almali - hicbiri saga'yi tekrar calistirmamali.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class IdempotencyKeyClaimConcurrencyTest {

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

    @Autowired
    private IdempotencyKeyService idempotencyKeyService;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Test
    void tryClaim_concurrentRequestsWithSameKey_onlyOneWinsTheClaim() throws Exception {
        String key = "order-idem-" + UUID.randomUUID();
        String requestHash = "same-request-hash";

        int concurrentRequests = 15;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch allThreadsReady = new CountDownLatch(concurrentRequests);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger claimedCount = new AtomicInteger();
        AtomicInteger inProgressCount = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executor.submit(() -> {
                allThreadsReady.countDown();
                startSignal.await();

                try {
                    Optional<Object> result = idempotencyKeyService.tryClaim(key, requestHash, Object.class);
                    if (result.isEmpty()) {
                        claimedCount.incrementAndGet();
                    }
                } catch (OrderProcessingInProgressException ex) {
                    inProgressCount.incrementAndGet();
                }
                return null;
            }));
        }

        allThreadsReady.await(5, TimeUnit.SECONDS);
        startSignal.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(claimedCount.get()).isEqualTo(1);
        assertThat(inProgressCount.get()).isEqualTo(concurrentRequests - 1);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
    }
}
