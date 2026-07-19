package com.turkcell.billing.service;

import com.turkcell.billing.dto.request.InvoiceCreateRequest;
import com.turkcell.billing.dto.request.InvoiceLineRequest;
import com.turkcell.billing.dto.response.InvoiceResponse;
import com.turkcell.billing.repository.InvoiceRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class InvoiceDuplicationConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("billing_db_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void createInvoice_concurrentRequestsForSameSubscriptionAndPeriod_onlyCreatesOneInvoice() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        LocalDate periodStart = LocalDate.of(2026, 7, 1);
        LocalDate periodEnd = LocalDate.of(2026, 7, 31);

        int concurrentRequests = 15;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch allThreadsReady = new CountDownLatch(concurrentRequests);
        CountDownLatch startSignal = new CountDownLatch(1);

        List<Future<InvoiceResponse>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executor.submit(() -> {
                allThreadsReady.countDown();
                startSignal.await();

                InvoiceCreateRequest request = new InvoiceCreateRequest();
                request.setCustomerId(UUID.randomUUID());
                request.setSubscriptionId(subscriptionId);
                request.setPeriodStart(periodStart);
                request.setPeriodEnd(periodEnd);
                request.setDueDate(periodEnd.plusDays(15));

                InvoiceLineRequest line = new InvoiceLineRequest();
                line.setDescription("Monthly fee");
                line.setQuantity(BigDecimal.ONE);
                line.setUnitPrice(new BigDecimal("100.00"));
                request.setLines(List.of(line));

                return invoiceService.createInvoice(request);
            }));
        }

        allThreadsReady.await(5, TimeUnit.SECONDS);
        startSignal.countDown();

        Set<UUID> resultingInvoiceIds = new HashSet<>();
        for (Future<InvoiceResponse> future : futures) {
            resultingInvoiceIds.add(future.get(10, TimeUnit.SECONDS).getId());
        }
        executor.shutdown();

        assertThat(resultingInvoiceIds).hasSize(1);
        assertThat(invoiceRepository.count()).isEqualTo(1);
    }
}
