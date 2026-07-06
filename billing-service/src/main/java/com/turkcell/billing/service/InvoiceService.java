package com.turkcell.billing.service;

import com.turkcell.billing.dto.request.InvoiceCreateRequest;
import com.turkcell.billing.dto.request.InvoiceLineRequest;
import com.turkcell.billing.dto.response.InvoiceResponse;
import com.turkcell.billing.entity.Invoice;
import com.turkcell.billing.entity.InvoiceLine;
import com.turkcell.billing.exception.InvalidInvoiceStateException;
import com.turkcell.billing.exception.InvoiceNotFoundException;
import com.turkcell.billing.mapper.InvoiceLineMapper;
import com.turkcell.billing.mapper.InvoiceMapper;
import com.turkcell.billing.repository.InvoiceLineRepository;
import com.turkcell.billing.repository.InvoiceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * musteri/abonelik dogrulugu bu servisin sorumlulugunda degildir: subscription-service/payment-service
 * ile ayni gerekceyle, bu kontrollerin cagiran (bill-run orkestrasyonu) tarafinda yapildigi varsayilir.
 */
@Service
public class InvoiceService {

    // MVP sadelestirmesi: BTK/vergi mevzuati detaylandirilmadigi icin tek bir sabit KDV orani kullanilir.
    private static final BigDecimal TAX_RATE = new BigDecimal("0.20");
    private static final String AGGREGATE_TYPE = "Invoice";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final BillCycleService billCycleService;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceLineMapper invoiceLineMapper;
    private final OutboxEventService outboxEventService;
    private final TransactionTemplate createInvoiceTransactionTemplate;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceLineRepository invoiceLineRepository,
                           BillCycleService billCycleService, InvoiceMapper invoiceMapper,
                           InvoiceLineMapper invoiceLineMapper, OutboxEventService outboxEventService,
                           PlatformTransactionManager transactionManager) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.billCycleService = billCycleService;
        this.invoiceMapper = invoiceMapper;
        this.invoiceLineMapper = invoiceLineMapper;
        this.outboxEventService = outboxEventService;

        // REQUIRES_NEW: payment-service/usage-service'teki idempotency dersinin ucuncu uygulamasi -
        // ayni (subscriptionId, periodStart, periodEnd) icin cift fatura kesilmesini engeller.
        // Postgres bir statement'ta hata verince tum transaction'i "aborted" isaretledigi icin riskli
        // insert kendi izole transaction'inda yapilir.
        this.createInvoiceTransactionTemplate = new TransactionTemplate(transactionManager);
        this.createInvoiceTransactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
        return findExisting(request)
                .map(this::toResponseWithLines)
                .orElseGet(() -> {
                    try {
                        return createInvoiceTransactionTemplate.execute(status -> processNewInvoice(request));
                    } catch (DataIntegrityViolationException ex) {
                        return findExisting(request)
                                .map(this::toResponseWithLines)
                                .orElseThrow(() -> ex);
                    }
                });
    }

    private java.util.Optional<Invoice> findExisting(InvoiceCreateRequest request) {
        return invoiceRepository.findBySubscriptionIdAndPeriodStartAndPeriodEnd(
                request.getSubscriptionId(), request.getPeriodStart(), request.getPeriodEnd());
    }

    private InvoiceResponse processNewInvoice(InvoiceCreateRequest request) {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(request.getCustomerId());
        invoice.setSubscriptionId(request.getSubscriptionId());
        invoice.setPeriodStart(request.getPeriodStart());
        invoice.setPeriodEnd(request.getPeriodEnd());
        invoice.setDueDate(request.getDueDate());
        invoice.setCurrency(request.getCurrency());
        invoice.setStatus(Invoice.STATUS_PENDING);

        BigDecimal subTotal = BigDecimal.ZERO;
        List<InvoiceLine> lines = new ArrayList<>();
        for (InvoiceLineRequest lineRequest : request.getLines()) {
            BigDecimal lineTotal = lineRequest.getQuantity().multiply(lineRequest.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            subTotal = subTotal.add(lineTotal);

            InvoiceLine line = new InvoiceLine();
            line.setDescription(lineRequest.getDescription());
            line.setQuantity(lineRequest.getQuantity());
            line.setUnitPrice(lineRequest.getUnitPrice());
            line.setLineTotal(lineTotal);
            lines.add(line);
        }

        BigDecimal tax = subTotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        invoice.setSubTotal(subTotal);
        invoice.setTax(tax);
        invoice.setGrandTotal(subTotal.add(tax));

        invoice = invoiceRepository.save(invoice);

        for (InvoiceLine line : lines) {
            line.setInvoiceId(invoice.getId());
        }
        invoiceLineRepository.saveAll(lines);

        billCycleService.advanceIfExists(request.getCustomerId());

        InvoiceResponse response = toResponse(invoice, lines);
        outboxEventService.publish(AGGREGATE_TYPE, invoice.getId(), "InvoiceGenerated", response);
        return response;
    }

    public InvoiceResponse getInvoiceResponseById(UUID id) {
        return toResponseWithLines(getInvoiceById(id));
    }

    public Invoice getInvoiceById(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with id: " + id));
    }

    public Page<InvoiceResponse> getInvoicesByCustomer(UUID customerId, Pageable pageable) {
        return invoiceRepository.findAllByCustomerId(customerId, pageable).map(this::toResponseWithLines);
    }

    @Transactional
    public InvoiceResponse markPaid(UUID id) {
        Invoice invoice = getInvoiceById(id);
        if (!Invoice.STATUS_PENDING.equals(invoice.getStatus()) && !Invoice.STATUS_OVERDUE.equals(invoice.getStatus())) {
            throw new InvalidInvoiceStateException(
                    "Only PENDING or OVERDUE invoices can be marked as paid, current status: " + invoice.getStatus());
        }
        invoice.setStatus(Invoice.STATUS_PAID);
        invoiceRepository.save(invoice);

        InvoiceResponse response = toResponseWithLines(invoice);
        outboxEventService.publish(AGGREGATE_TYPE, invoice.getId(), "InvoicePaid", response);
        return response;
    }

    @Transactional
    public InvoiceResponse markOverdue(UUID id) {
        Invoice invoice = getInvoiceById(id);
        if (!Invoice.STATUS_PENDING.equals(invoice.getStatus())) {
            throw new InvalidInvoiceStateException(
                    "Only PENDING invoices can be marked overdue, current status: " + invoice.getStatus());
        }
        invoice.setStatus(Invoice.STATUS_OVERDUE);
        invoiceRepository.save(invoice);

        InvoiceResponse response = toResponseWithLines(invoice);
        outboxEventService.publish(AGGREGATE_TYPE, invoice.getId(), "InvoiceOverdue", response);
        return response;
    }

    private InvoiceResponse toResponseWithLines(Invoice invoice) {
        return toResponse(invoice, invoiceLineRepository.findAllByInvoiceId(invoice.getId()));
    }

    private InvoiceResponse toResponse(Invoice invoice, List<InvoiceLine> lines) {
        InvoiceResponse response = invoiceMapper.toResponse(invoice);
        response.setLines(lines.stream().map(invoiceLineMapper::toResponse).collect(Collectors.toList()));
        return response;
    }
}
