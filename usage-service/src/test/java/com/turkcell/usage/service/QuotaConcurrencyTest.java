package com.turkcell.usage.service;

import com.turkcell.usage.dto.request.QuotaCreateRequest;
import com.turkcell.usage.repository.QuotaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class QuotaConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("usage_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private QuotaRepository quotaRepository;

    @Test
    void deduct_underConcurrentRequests_neverLosesAnUpdate() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        QuotaCreateRequest createRequest = new QuotaCreateRequest();
        createRequest.setSubscriptionId(subscriptionId);
        createRequest.setPeriodStart(LocalDate.now());
        createRequest.setPeriodEnd(LocalDate.now().plusDays(30));
        createRequest.setMinutesIncluded(1000);
        createRequest.setSmsIncluded(1000);
        createRequest.setMbIncluded(1000);
        quotaService.createQuota(createRequest);

        int concurrentDeductions = 25;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentDeductions);
        CountDownLatch allThreadsReady = new CountDownLatch(concurrentDeductions);
        CountDownLatch startSignal = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentDeductions; i++) {
            futures.add(executor.submit(() -> {
                allThreadsReady.countDown();
                startSignal.await();
                quotaService.deduct(subscriptionId, "VOICE", BigDecimal.ONE);
                return null;
            }));
        }

        allThreadsReady.await(5, TimeUnit.SECONDS);
        startSignal.countDown();
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        int remaining = quotaRepository.findBySubscriptionId(subscriptionId).orElseThrow().getMinutesRemaining();
        assertThat(remaining).isEqualTo(1000 - concurrentDeductions);
    }
}
