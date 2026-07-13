package com.turkcell.billing.service;

import com.turkcell.billing.client.ProductCatalogServiceClient;
import com.turkcell.billing.client.TariffClientDto;
import com.turkcell.billing.dto.request.InvoiceCreateRequest;
import com.turkcell.billing.dto.request.InvoiceLineRequest;
import com.turkcell.billing.dto.response.InvoiceResponse;
import com.turkcell.billing.entity.Invoice;
import com.turkcell.billing.entity.InvoiceLine;
import com.turkcell.billing.entity.UsageAggregate;
import com.turkcell.billing.exception.InvalidInvoiceStateException;
import com.turkcell.billing.exception.InvoiceNotFoundException;
import com.turkcell.billing.mapper.InvoiceLineMapper;
import com.turkcell.billing.mapper.InvoiceMapper;
import com.turkcell.billing.repository.InvoiceLineRepository;
import com.turkcell.billing.repository.InvoiceRepository;
import com.turkcell.billing.repository.UsageAggregateRepository;
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
import java.util.Map;
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
    private final UsageAggregateRepository usageAggregateRepository;
    private final BillCycleService billCycleService;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceLineMapper invoiceLineMapper;
    private final OutboxEventService outboxEventService;
    private final ProductCatalogServiceClient productCatalogServiceClient;
    private final TransactionTemplate createInvoiceTransactionTemplate;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceLineRepository invoiceLineRepository,
                           UsageAggregateRepository usageAggregateRepository, BillCycleService billCycleService,
                           InvoiceMapper invoiceMapper, InvoiceLineMapper invoiceLineMapper,
                           OutboxEventService outboxEventService, ProductCatalogServiceClient productCatalogServiceClient,
                           PlatformTransactionManager transactionManager) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.usageAggregateRepository = usageAggregateRepository;
        this.billCycleService = billCycleService;
        this.invoiceMapper = invoiceMapper;
        this.invoiceLineMapper = invoiceLineMapper;
        this.outboxEventService = outboxEventService;
        this.productCatalogServiceClient = productCatalogServiceClient;

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
        invoice.setTariffCode(request.getTariffCode());
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

        List<UsageAggregate> unclaimed = findUnclaimedOverage(request);
        List<UsageAggregate> claimedOverage = new ArrayList<>();
        if (!unclaimed.isEmpty()) {
            TariffClientDto tariff = productCatalogServiceClient.getTariff(request.getTariffCode());
            for (Map.Entry<String, List<UsageAggregate>> entry : groupByType(unclaimed).entrySet()) {
                BigDecimal totalQuantity = entry.getValue().stream()
                        .map(UsageAggregate::getOverageQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                InvoiceLine overageLine = buildOverageLine(entry.getKey(), totalQuantity, tariff);
                if (overageLine == null) {
                    // Oran tanimli/pozitif degil - bu grup faturalanmaz VE claim edilmez, boylece
                    // ileride tarifeye oran eklenirse ayni asim hala islenebilir kalir.
                    continue;
                }
                subTotal = subTotal.add(overageLine.getLineTotal());
                lines.add(overageLine);
                claimedOverage.addAll(entry.getValue());
            }
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

        if (!claimedOverage.isEmpty()) {
            for (UsageAggregate aggregate : claimedOverage) {
                aggregate.setInvoiceId(invoice.getId());
            }
            usageAggregateRepository.saveAll(claimedOverage);
        }

        billCycleService.advanceIfExists(request.getCustomerId());

        InvoiceResponse response = toResponse(invoice, lines);
        outboxEventService.publish(AGGREGATE_TYPE, invoice.getId(), "InvoiceGenerated", response);
        return response;
    }

    /**
     * tariffCode bos birakilirsa asim hesaplamasi tamamen atlanir (geriye donuk uyumlu - mevcut
     * davranis degismez). Doluysa, ayni abonelik+donem icin henuz faturalanmamis (invoiceId NULL)
     * usage_aggregates satirlari doner - period alanlari Invoice'in donemiyle birebir eslesmeli
     * (usage-service'in Quota donemiyle bill-run'in fatura donemi ayni varsayilir).
     */
    private List<UsageAggregate> findUnclaimedOverage(InvoiceCreateRequest request) {
        if (request.getTariffCode() == null) {
            return List.of();
        }
        return usageAggregateRepository.findBySubscriptionIdAndPeriodStartAndPeriodEndAndInvoiceIdIsNull(
                request.getSubscriptionId(), request.getPeriodStart(), request.getPeriodEnd());
    }

    /**
     * Ayni donemde ayni cdrType icin birden fazla UsageAggregated event'i gelmis olabilir (orn. iki
     * ayri CDR ayni ay icinde asima neden olmus) - tek bir birlesik satirda toplanir, faturayi
     * gereksiz satirlarla kalabalıklastirmaz. Orijinal UsageAggregate referanslari saklanir ki
     * hangilerinin gercekten faturalandigi (claim edildigi) sonradan belirlenebilsin.
     */
    private Map<String, List<UsageAggregate>> groupByType(List<UsageAggregate> unclaimed) {
        return unclaimed.stream().collect(Collectors.groupingBy(UsageAggregate::getCdrType));
    }

    private InvoiceLine buildOverageLine(String cdrType, BigDecimal totalQuantity, TariffClientDto tariff) {
        BigDecimal rate = overageRateFor(cdrType, tariff);
        if (rate == null || rate.signum() <= 0 || totalQuantity.signum() <= 0) {
            return null;
        }

        InvoiceLine line = new InvoiceLine();
        line.setDescription(overageDescription(cdrType) + " (" + totalQuantity + " " + overageUnit(cdrType) + " asim)");
        line.setQuantity(totalQuantity);
        line.setUnitPrice(rate);
        line.setLineTotal(totalQuantity.multiply(rate).setScale(2, RoundingMode.HALF_UP));
        return line;
    }

    private BigDecimal overageRateFor(String cdrType, TariffClientDto tariff) {
        return switch (cdrType) {
            case "VOICE" -> tariff.getOverageRatePerMinute();
            case "SMS" -> tariff.getOverageRateSms();
            case "DATA" -> tariff.getOverageRatePerMb();
            default -> null;
        };
    }

    private String overageDescription(String cdrType) {
        return switch (cdrType) {
            case "VOICE" -> "Ses asim ucreti";
            case "SMS" -> "SMS asim ucreti";
            case "DATA" -> "Data asim ucreti";
            default -> cdrType + " asim ucreti";
        };
    }

    private String overageUnit(String cdrType) {
        return switch (cdrType) {
            case "VOICE" -> "dakika";
            case "SMS" -> "adet";
            case "DATA" -> "MB";
            default -> "birim";
        };
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
