package com.turkcell.usage.service;

import com.turkcell.usage.dto.request.CdrEventIngestRequest;
import com.turkcell.usage.dto.request.QuotaCreateRequest;
import com.turkcell.usage.dto.response.CdrEventResponse;
import com.turkcell.usage.repository.CdrEventRepository;
import com.turkcell.usage.repository.UsageRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gercek CDR mediation sistemleri en-az-bir-kez (at-least-once) teslimat yapar; ayni externalCdrId
 * birden fazla gelebilir. Bu test, ayni anda cok sayida istek AYNI externalCdrId ile geldiginde
 * sadece TEK bir CdrEvent/UsageRecord olustugunu ve kotanin sadece BIR KEZ dustugunu gercek Postgres'e
 * karsi dogrular (payment-service'teki idempotency testiyle ayni sinif problem).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class CdrIdempotencyConcurrencyTest {

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
    private CdrEventService cdrEventService;

    @Autowired
    private CdrEventRepository cdrEventRepository;

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Test
    void ingest_concurrentRequestsWithSameExternalCdrId_onlyProcessesOnce() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        QuotaCreateRequest quotaRequest = new QuotaCreateRequest();
        quotaRequest.setSubscriptionId(subscriptionId);
        quotaRequest.setPeriodStart(LocalDate.now());
        quotaRequest.setPeriodEnd(LocalDate.now().plusDays(30));
        quotaRequest.setMinutesIncluded(1000);
        quotaRequest.setSmsIncluded(1000);
        quotaRequest.setMbIncluded(1000);
        quotaService.createQuota(quotaRequest);

        String externalCdrId = "concurrent-cdr-" + UUID.randomUUID();
        int concurrentRequests = 15;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch allThreadsReady = new CountDownLatch(concurrentRequests);
        CountDownLatch startSignal = new CountDownLatch(1);

        List<Future<CdrEventResponse>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executor.submit(() -> {
                allThreadsReady.countDown();
                startSignal.await();
                CdrEventIngestRequest request = new CdrEventIngestRequest();
                request.setExternalCdrId(externalCdrId);
                request.setSubscriptionId(subscriptionId);
                request.setMsisdn("905550000001");
                request.setCdrType("VOICE");
                request.setStartTime(Instant.now());
                request.setDurationSeconds(60);
                return cdrEventService.ingest(request);
            }));
        }

        allThreadsReady.await(5, TimeUnit.SECONDS);
        startSignal.countDown();

        Set<UUID> resultingCdrEventIds = new HashSet<>();
        for (Future<CdrEventResponse> future : futures) {
            resultingCdrEventIds.add(future.get(10, TimeUnit.SECONDS).getId());
        }
        executor.shutdown();

        assertThat(resultingCdrEventIds).hasSize(1);
        assertThat(cdrEventRepository.count()).isEqualTo(1);
        assertThat(usageRecordRepository.count()).isEqualTo(1);

        int remaining = quotaService.getQuotaBySubscriptionId(subscriptionId).getMinutesRemaining();
        assertThat(remaining).isEqualTo(999); // sadece 1 dakika dusulmus olmali, 15 kez degil
    }
}
