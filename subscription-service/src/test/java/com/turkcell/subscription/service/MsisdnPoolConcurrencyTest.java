package com.turkcell.subscription.service;

import com.turkcell.subscription.repository.MsisdnPoolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class MsisdnPoolConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("subscription_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MsisdnPoolService msisdnPoolService;

    @Autowired
    private MsisdnPoolRepository msisdnPoolRepository;

    @Test
    void allocateNext_underConcurrentRequests_neverAllocatesSameMsisdnTwice() throws Exception {
        int concurrentRequests = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch allThreadsReady = new CountDownLatch(concurrentRequests);
        CountDownLatch startSignal = new CountDownLatch(1);

        java.util.List<Future<String>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executor.submit(() -> {
                allThreadsReady.countDown();
                startSignal.await();
                return msisdnPoolService.allocateNext();
            }));
        }

        allThreadsReady.await(5, TimeUnit.SECONDS);
        startSignal.countDown();

        Set<String> allocatedNumbers = new HashSet<>();
        for (Future<String> future : futures) {
            allocatedNumbers.add(future.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        assertThat(allocatedNumbers).hasSize(concurrentRequests);
        long allocatedInDb = msisdnPoolRepository.findAllByStatus("ALLOCATED", Pageable.unpaged()).getTotalElements();
        assertThat(allocatedInDb).isEqualTo(concurrentRequests);
    }
}
