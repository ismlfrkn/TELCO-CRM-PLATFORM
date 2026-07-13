package com.turkcell.billing.service;

import com.turkcell.billing.client.PageResponse;
import com.turkcell.billing.client.ProductCatalogServiceClient;
import com.turkcell.billing.client.SubscriptionClientDto;
import com.turkcell.billing.client.SubscriptionServiceClient;
import com.turkcell.billing.client.TariffClientDto;
import com.turkcell.billing.dto.request.BillingRunAutoRequest;
import com.turkcell.billing.dto.request.BillingRunRequest;
import com.turkcell.billing.dto.request.InvoiceCreateRequest;
import com.turkcell.billing.dto.request.InvoiceLineRequest;
import com.turkcell.billing.dto.response.BillingRunResponse;
import com.turkcell.billing.dto.response.InvoiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillingRunServiceTest {

    private InvoiceService invoiceService;
    private SubscriptionServiceClient subscriptionServiceClient;
    private ProductCatalogServiceClient productCatalogServiceClient;
    private BillingRunService billingRunService;

    @BeforeEach
    void setUp() {
        invoiceService = mock(InvoiceService.class);
        subscriptionServiceClient = mock(SubscriptionServiceClient.class);
        productCatalogServiceClient = mock(ProductCatalogServiceClient.class);
        billingRunService = new BillingRunService(invoiceService, subscriptionServiceClient, productCatalogServiceClient);

        when(invoiceService.createInvoice(any())).thenAnswer(inv -> {
            InvoiceResponse response = new InvoiceResponse();
            response.setId(UUID.randomUUID());
            return response;
        });
    }

    @Test
    void run_processesEveryInvoiceRequestAndAggregatesResults() {
        BillingRunRequest request = new BillingRunRequest();
        request.setInvoices(List.of(invoiceRequest(), invoiceRequest(), invoiceRequest()));

        BillingRunResponse response = billingRunService.run(request);

        assertThat(response.getRequested()).isEqualTo(3);
        assertThat(response.getProcessed()).isEqualTo(3);
        assertThat(response.getInvoices()).hasSize(3);
        verify(invoiceService, times(3)).createInvoice(any());
    }

    @Test
    void runAutomatic_derivesInvoiceRequestFromActiveSubscriptionsAndTariff() {
        UUID subscriptionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        SubscriptionClientDto subscription = subscriptionOf(subscriptionId, customerId, "STD-POSTPAID-100");
        when(subscriptionServiceClient.getActiveSubscriptions(any(Pageable.class)))
                .thenReturn(pageOf(List.of(subscription), true));

        TariffClientDto tariff = tariffOf("STD-POSTPAID-100", "Standart 100", "149.90", "TRY");
        when(productCatalogServiceClient.getTariff("STD-POSTPAID-100")).thenReturn(tariff);

        BillingRunAutoRequest request = autoRequest();
        BillingRunResponse response = billingRunService.runAutomatic(request);

        assertThat(response.getRequested()).isEqualTo(1);
        assertThat(response.getProcessed()).isEqualTo(1);

        ArgumentCaptor<InvoiceCreateRequest> captor = ArgumentCaptor.forClass(InvoiceCreateRequest.class);
        verify(invoiceService).createInvoice(captor.capture());
        InvoiceCreateRequest built = captor.getValue();
        assertThat(built.getCustomerId()).isEqualTo(customerId);
        assertThat(built.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(built.getTariffCode()).isEqualTo("STD-POSTPAID-100");
        assertThat(built.getCurrency()).isEqualTo("TRY");
        assertThat(built.getLines()).hasSize(1);
        assertThat(built.getLines().get(0).getUnitPrice()).isEqualByComparingTo("149.90");
        assertThat(built.getLines().get(0).getDescription()).contains("Standart 100");
    }

    @Test
    void runAutomatic_pagesThroughAllActiveSubscriptions() {
        SubscriptionClientDto first = subscriptionOf(UUID.randomUUID(), UUID.randomUUID(), "TRF-A");
        SubscriptionClientDto second = subscriptionOf(UUID.randomUUID(), UUID.randomUUID(), "TRF-B");
        when(subscriptionServiceClient.getActiveSubscriptions(any(Pageable.class)))
                .thenReturn(pageOf(List.of(first), false))
                .thenReturn(pageOf(List.of(second), true));
        when(productCatalogServiceClient.getTariff(any())).thenReturn(tariffOf("TRF-X", "Tarife", "100.00", "TRY"));

        BillingRunResponse response = billingRunService.runAutomatic(autoRequest());

        assertThat(response.getRequested()).isEqualTo(2);
        assertThat(response.getProcessed()).isEqualTo(2);
        verify(invoiceService, times(2)).createInvoice(any());
    }

    @Test
    void runAutomatic_withNoActiveSubscriptions_returnsEmptyResult() {
        when(subscriptionServiceClient.getActiveSubscriptions(any(Pageable.class)))
                .thenReturn(pageOf(List.of(), true));

        BillingRunResponse response = billingRunService.runAutomatic(autoRequest());

        assertThat(response.getRequested()).isZero();
        assertThat(response.getProcessed()).isZero();
        verifyNoInteractions(invoiceService);
    }

    private BillingRunAutoRequest autoRequest() {
        BillingRunAutoRequest request = new BillingRunAutoRequest();
        request.setPeriodStart(LocalDate.of(2026, 7, 1));
        request.setPeriodEnd(LocalDate.of(2026, 7, 31));
        request.setDueDate(LocalDate.of(2026, 8, 15));
        return request;
    }

    private SubscriptionClientDto subscriptionOf(UUID id, UUID customerId, String tariffCode) {
        SubscriptionClientDto dto = new SubscriptionClientDto();
        dto.setId(id);
        dto.setCustomerId(customerId);
        dto.setTariffCode(tariffCode);
        dto.setStatus("ACTIVE");
        return dto;
    }

    private TariffClientDto tariffOf(String code, String name, String monthlyFee, String currency) {
        TariffClientDto dto = new TariffClientDto();
        dto.setCode(code);
        dto.setName(name);
        dto.setMonthlyFee(new BigDecimal(monthlyFee));
        dto.setCurrency(currency);
        return dto;
    }

    private PageResponse<SubscriptionClientDto> pageOf(List<SubscriptionClientDto> content, boolean last) {
        PageResponse<SubscriptionClientDto> page = new PageResponse<>();
        page.setContent(content);
        page.setLast(last);
        return page;
    }

    private InvoiceCreateRequest invoiceRequest() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setSubscriptionId(UUID.randomUUID());
        request.setPeriodStart(LocalDate.of(2026, 7, 1));
        request.setPeriodEnd(LocalDate.of(2026, 7, 31));
        request.setDueDate(LocalDate.of(2026, 8, 15));

        InvoiceLineRequest line = new InvoiceLineRequest();
        line.setDescription("Monthly fee");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100.00"));
        request.setLines(List.of(line));
        return request;
    }
}
