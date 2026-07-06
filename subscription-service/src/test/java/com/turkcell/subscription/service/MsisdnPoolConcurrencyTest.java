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

/**
 * Mockito ile FOR UPDATE SKIP LOCKED'in gercekten calistigini kanitlayamayiz - o sadece Postgres'in
 * kendi kilitleme davranisi. Bu yuzden bu test gercek bir Postgres container'ina karsi, ayni anda
 * cok sayida allocateNext() cagirip hicbir MSISDN'in iki abonelige birden verilmedigini dogrular.
 */
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
        int concurrentRequests = 20; // V2 migration havuza 200 FREE numara seed'ler, yeterli
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
        startSignal.countDown(); // hepsini ayni anda serbest birak

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
