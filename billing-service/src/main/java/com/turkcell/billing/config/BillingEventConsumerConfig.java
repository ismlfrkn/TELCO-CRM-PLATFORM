package com.turkcell.billing.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.billing.dto.request.BillCycleCreateRequest;
import com.turkcell.billing.entity.UsageAggregate;
import com.turkcell.billing.exception.DuplicateBillCycleException;
import com.turkcell.billing.exception.InvalidInvoiceStateException;
import com.turkcell.billing.repository.UsageAggregateRepository;
import com.turkcell.billing.service.BillCycleService;
import com.turkcell.billing.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
public class BillingEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(BillingEventConsumerConfig.class);

    private final BillCycleService billCycleService;
    private final InvoiceService invoiceService;
    private final UsageAggregateRepository usageAggregateRepository;
    private final ObjectMapper objectMapper;

    public BillingEventConsumerConfig(BillCycleService billCycleService, InvoiceService invoiceService,
                                       UsageAggregateRepository usageAggregateRepository, ObjectMapper objectMapper) {
        this.billCycleService = billCycleService;
        this.invoiceService = invoiceService;
        this.usageAggregateRepository = usageAggregateRepository;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Consumer<Message<String>> subscriptionEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message, "telco.subscription.events");
            if (envelope == null) {
                return;
            }
            if (!"SubscriptionActivated".equals(envelope.get("eventType").asText())) {
                return;
            }

            JsonNode payload = envelope.get("payload");
            UUID customerId = UUID.fromString(payload.get("customerId").asText());
            int dayOfMonth = dayOfMonthFrom(payload);

            BillCycleCreateRequest request = new BillCycleCreateRequest();
            request.setCustomerId(customerId);
            request.setDayOfMonth(dayOfMonth);

            try {
                billCycleService.createBillCycle(request);
            } catch (DuplicateBillCycleException ex) {
                log.info("Customer {} already has a bill cycle, ignoring SubscriptionActivated", customerId);
            }
        };
    }

    @Bean
    public Consumer<Message<String>> paymentEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message, "telco.payment.events");
            if (envelope == null) {
                return;
            }
            if (!"PaymentCompleted".equals(envelope.get("eventType").asText())) {
                return;
            }

            JsonNode payload = envelope.get("payload");
            JsonNode invoiceIdNode = payload.get("invoiceId");
            if (invoiceIdNode == null || invoiceIdNode.isNull()) {

                return;
            }

            UUID invoiceId = UUID.fromString(invoiceIdNode.asText());
            try {
                invoiceService.markPaid(invoiceId);
            } catch (InvalidInvoiceStateException ex) {
                log.info("Invoice {} already not payable ({}), ignoring duplicate PaymentCompleted",
                        invoiceId, ex.getMessage());
            }
        };
    }

    @Bean
    public Consumer<Message<String>> usageEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message, "telco.usage.events");
            if (envelope == null) {
                return;
            }
            if (!"UsageAggregated".equals(envelope.get("eventType").asText())) {
                return;
            }

            UUID sourceEventId = UUID.fromString(envelope.get("eventId").asText());
            JsonNode payload = envelope.get("payload");

            UsageAggregate aggregate = new UsageAggregate();
            aggregate.setSourceEventId(sourceEventId);
            aggregate.setSubscriptionId(UUID.fromString(payload.get("subscriptionId").asText()));
            aggregate.setQuotaId(payload.hasNonNull("quotaId") ? UUID.fromString(payload.get("quotaId").asText()) : null);
            aggregate.setCdrType(payload.get("cdrType").asText());
            aggregate.setOverageQuantity(new BigDecimal(payload.get("overageQuantity").asText()));
            aggregate.setPeriodStart(payload.hasNonNull("periodStart") ? LocalDate.parse(payload.get("periodStart").asText()) : null);
            aggregate.setPeriodEnd(payload.hasNonNull("periodEnd") ? LocalDate.parse(payload.get("periodEnd").asText()) : null);

            try {
                usageAggregateRepository.save(aggregate);
            } catch (DataIntegrityViolationException ex) {
                log.info("UsageAggregated event {} already recorded, ignoring duplicate delivery", sourceEventId);
            }
        };
    }

    private int dayOfMonthFrom(JsonNode payload) {
        if (payload.hasNonNull("activatedAt")) {
            int day = Instant.parse(payload.get("activatedAt").asText()).atZone(ZoneOffset.UTC).getDayOfMonth();
            return Math.min(day, 28);
        }
        return Math.min(LocalDate.now().getDayOfMonth(), 28);
    }

    private JsonNode readEnvelope(Message<String> message, String sourceTopic) {
        try {
            return objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            log.error("Malformed event envelope from {}: {}", sourceTopic, message.getPayload(), ex);
            return null;
        }
    }
}
