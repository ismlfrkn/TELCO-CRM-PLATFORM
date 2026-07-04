package com.turkcell.billing.service;

import com.turkcell.billing.dto.request.BillingRunRequest;
import com.turkcell.billing.dto.request.InvoiceCreateRequest;
import com.turkcell.billing.dto.request.InvoiceLineRequest;
import com.turkcell.billing.dto.response.BillingRunResponse;
import com.turkcell.billing.dto.response.InvoiceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillingRunServiceTest {

    private InvoiceService invoiceService;
    private BillingRunService billingRunService;

    @BeforeEach
    void setUp() {
        invoiceService = mock(InvoiceService.class);
        billingRunService = new BillingRunService(invoiceService);
    }

    @Test
    void run_processesEveryInvoiceRequestAndAggregatesResults() {
        when(invoiceService.createInvoice(any())).thenAnswer(inv -> {
            InvoiceResponse response = new InvoiceResponse();
            response.setId(UUID.randomUUID());
            return response;
        });

        BillingRunRequest request = new BillingRunRequest();
        request.setInvoices(List.of(invoiceRequest(), invoiceRequest(), invoiceRequest()));

        BillingRunResponse response = billingRunService.run(request);

        assertThat(response.getRequested()).isEqualTo(3);
        assertThat(response.getProcessed()).isEqualTo(3);
        assertThat(response.getInvoices()).hasSize(3);
        verify(invoiceService, times(3)).createInvoice(any());
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
