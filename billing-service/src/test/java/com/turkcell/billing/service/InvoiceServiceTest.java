package com.turkcell.billing.service;

import com.turkcell.billing.dto.request.InvoiceCreateRequest;
import com.turkcell.billing.dto.request.InvoiceLineRequest;
import com.turkcell.billing.dto.response.InvoiceResponse;
import com.turkcell.billing.entity.Invoice;
import com.turkcell.billing.exception.InvalidInvoiceStateException;
import com.turkcell.billing.exception.InvoiceNotFoundException;
import com.turkcell.billing.mapper.InvoiceLineMapper;
import com.turkcell.billing.mapper.InvoiceMapper;
import com.turkcell.billing.repository.InvoiceLineRepository;
import com.turkcell.billing.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceServiceTest {

    private InvoiceRepository invoiceRepository;
    private InvoiceLineRepository invoiceLineRepository;
    private BillCycleService billCycleService;
    private OutboxEventService outboxEventService;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        invoiceLineRepository = mock(InvoiceLineRepository.class);
        billCycleService = mock(BillCycleService.class);
        outboxEventService = mock(OutboxEventService.class);
        InvoiceMapper invoiceMapper = Mappers.getMapper(InvoiceMapper.class);
        InvoiceLineMapper invoiceLineMapper = Mappers.getMapper(InvoiceLineMapper.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

        invoiceService = new InvoiceService(invoiceRepository, invoiceLineRepository, billCycleService,
                invoiceMapper, invoiceLineMapper, outboxEventService, transactionManager);

        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            if (invoice.getId() == null) {
                invoice.setId(UUID.randomUUID());
            }
            return invoice;
        });
        when(invoiceLineRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceLineRepository.findAllByInvoiceId(any())).thenReturn(List.of());
    }

    @Test
    void createInvoice_computesSubTotalTaxAndGrandTotalFromLines() {
        UUID subscriptionId = UUID.randomUUID();
        when(invoiceRepository.findBySubscriptionIdAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .thenReturn(Optional.empty());

        InvoiceCreateRequest request = requestWithLines(subscriptionId,
                lineOf("Monthly fee", "1", "150.00"),
                lineOf("Extra 5GB addon", "1", "50.00"));

        InvoiceResponse response = invoiceService.createInvoice(request);

        assertThat(response.getSubTotal()).isEqualByComparingTo("200.00");
        assertThat(response.getTax()).isEqualByComparingTo("40.00"); // %20 KDV
        assertThat(response.getGrandTotal()).isEqualByComparingTo("240.00");
        assertThat(response.getStatus()).isEqualTo(Invoice.STATUS_PENDING);
        verify(outboxEventService).publish(eq("Invoice"), any(), eq("InvoiceGenerated"), any());
    }

    @Test
    void createInvoice_withExplicitCurrency_propagatesCurrencyToInvoice() {
        UUID subscriptionId = UUID.randomUUID();
        when(invoiceRepository.findBySubscriptionIdAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .thenReturn(Optional.empty());

        InvoiceCreateRequest request = requestWithLines(subscriptionId, lineOf("Monthly fee", "1", "100.00"));
        request.setCurrency("EUR");

        InvoiceResponse response = invoiceService.createInvoice(request);

        assertThat(response.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void createInvoice_advancesBillCycleForCustomer() {
        UUID subscriptionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(invoiceRepository.findBySubscriptionIdAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .thenReturn(Optional.empty());

        InvoiceCreateRequest request = requestWithLines(subscriptionId, lineOf("Monthly fee", "1", "100.00"));
        request.setCustomerId(customerId);

        invoiceService.createInvoice(request);

        verify(billCycleService).advanceIfExists(customerId);
    }

    @Test
    void createInvoice_withSameSubscriptionAndPeriod_returnsExistingWithoutReprocessing() {
        UUID subscriptionId = UUID.randomUUID();
        LocalDate periodStart = LocalDate.of(2026, 7, 1);
        LocalDate periodEnd = LocalDate.of(2026, 7, 31);

        Invoice existing = existingInvoice(subscriptionId, periodStart, periodEnd);
        when(invoiceRepository.findBySubscriptionIdAndPeriodStartAndPeriodEnd(subscriptionId, periodStart, periodEnd))
                .thenReturn(Optional.of(existing));

        InvoiceCreateRequest request = requestWithLines(subscriptionId, lineOf("Monthly fee", "1", "100.00"));
        request.setPeriodStart(periodStart);
        request.setPeriodEnd(periodEnd);

        InvoiceResponse response = invoiceService.createInvoice(request);

        assertThat(response.getId()).isEqualTo(existing.getId());
        verifyNoInteractions(billCycleService);
        verifyNoInteractions(outboxEventService);
    }

    @Test
    void markPaid_fromPending_succeedsAndPublishesInvoicePaid() {
        Invoice invoice = existingInvoice(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30));
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.markPaid(invoice.getId());

        assertThat(response.getStatus()).isEqualTo(Invoice.STATUS_PAID);
        verify(outboxEventService).publish(eq("Invoice"), eq(invoice.getId()), eq("InvoicePaid"), any());
    }

    @Test
    void markPaid_fromOverdue_succeeds() {
        Invoice invoice = existingInvoice(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30));
        invoice.setStatus(Invoice.STATUS_OVERDUE);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.markPaid(invoice.getId());

        assertThat(response.getStatus()).isEqualTo(Invoice.STATUS_PAID);
    }

    @Test
    void markPaid_whenAlreadyPaid_throwsInvalidInvoiceStateException() {
        Invoice invoice = existingInvoice(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30));
        invoice.setStatus(Invoice.STATUS_PAID);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.markPaid(invoice.getId()))
                .isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void markOverdue_fromPending_succeedsAndPublishesInvoiceOverdue() {
        Invoice invoice = existingInvoice(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30));
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.markOverdue(invoice.getId());

        assertThat(response.getStatus()).isEqualTo(Invoice.STATUS_OVERDUE);
        verify(outboxEventService).publish(eq("Invoice"), eq(invoice.getId()), eq("InvoiceOverdue"), any());
    }

    @Test
    void markOverdue_whenAlreadyPaid_throwsInvalidInvoiceStateException() {
        Invoice invoice = existingInvoice(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30));
        invoice.setStatus(Invoice.STATUS_PAID);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.markOverdue(invoice.getId()))
                .isInstanceOf(InvalidInvoiceStateException.class);
    }

    @Test
    void getInvoiceById_whenMissing_throwsInvoiceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(id)).isInstanceOf(InvoiceNotFoundException.class);
    }

    private InvoiceCreateRequest requestWithLines(UUID subscriptionId, InvoiceLineRequest... lines) {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setCustomerId(UUID.randomUUID());
        request.setSubscriptionId(subscriptionId);
        request.setPeriodStart(LocalDate.of(2026, 7, 1));
        request.setPeriodEnd(LocalDate.of(2026, 7, 31));
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setLines(List.of(lines));
        return request;
    }

    private InvoiceLineRequest lineOf(String description, String quantity, String unitPrice) {
        InvoiceLineRequest line = new InvoiceLineRequest();
        line.setDescription(description);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal(unitPrice));
        return line;
    }

    private Invoice existingInvoice(UUID subscriptionId, LocalDate periodStart, LocalDate periodEnd) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setSubscriptionId(subscriptionId);
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setSubTotal(new BigDecimal("100.00"));
        invoice.setTax(new BigDecimal("20.00"));
        invoice.setGrandTotal(new BigDecimal("120.00"));
        invoice.setStatus(Invoice.STATUS_PENDING);
        invoice.setDueDate(periodEnd.plusDays(15));
        invoice.setIssuedAt(Instant.now());
        return invoice;
    }
}
