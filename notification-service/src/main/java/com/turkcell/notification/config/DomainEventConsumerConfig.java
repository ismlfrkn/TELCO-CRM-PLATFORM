package com.turkcell.notification.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.notification.dto.request.NotificationSendRequest;
import com.turkcell.notification.service.EventIdempotencyService;
import com.turkcell.notification.service.MockNotificationDispatcher;
import com.turkcell.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
public class DomainEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumerConfig.class);

    private static final Map<String, TemplateMapping> EVENT_TEMPLATES = Map.of(
            "CustomerRegistered", new TemplateMapping("CUSTOMER_WELCOME_SMS", "SMS"),
            "SubscriptionActivated", new TemplateMapping("WELCOME_SMS", "SMS"),
            "InvoiceGenerated", new TemplateMapping("INVOICE_GENERATED_EMAIL", "EMAIL"),
            "OrderCancelled", new TemplateMapping("ORDER_CANCELLED_SMS", "SMS"),
            "PaymentFailed", new TemplateMapping("PAYMENT_FAILED_SMS", "SMS")
    );

    private final EventIdempotencyService eventIdempotencyService;
    private final MockNotificationDispatcher mockNotificationDispatcher;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public DomainEventConsumerConfig(EventIdempotencyService eventIdempotencyService,
                                      MockNotificationDispatcher mockNotificationDispatcher,
                                      NotificationService notificationService,
                                      ObjectMapper objectMapper) {
        this.eventIdempotencyService = eventIdempotencyService;
        this.mockNotificationDispatcher = mockNotificationDispatcher;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Consumer<Message<String>> customerEvents() {
        return message -> handle("telco.customer.events", message, null);
    }

    @Bean
    public Consumer<Message<String>> ticketEvents() {
        return message -> handle("telco.ticket.events", message, null);
    }

    @Bean
    public Consumer<Message<String>> catalogEvents() {
        return message -> handle("telco.catalog.events", message, null);
    }

    @Bean
    public Consumer<Message<String>> subscriptionEvents() {
        return message -> handle("telco.subscription.events", message, "SubscriptionActivated");
    }

    @Bean
    public Consumer<Message<String>> billingEvents() {
        return message -> handle("telco.billing.events", message, "InvoiceGenerated");
    }

    @Bean
    public Consumer<Message<String>> orderEvents() {
        return message -> handle("telco.order.events", message, "OrderCancelled");
    }

    @Bean
    public Consumer<Message<String>> paymentEvents() {
        return message -> handle("telco.payment.events", message, "PaymentFailed");
    }

    @Bean
    public Consumer<Message<String>> usageEvents() {
        return message -> handleQuotaEvent("telco.usage.events", message);
    }

    private void handleQuotaEvent(String sourceTopic, Message<String> message) {
        JsonNode envelope;
        try {
            envelope = objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            log.error("Malformed event envelope from {}: {}", sourceTopic, message.getPayload(), ex);
            return;
        }

        String eventType = envelope.get("eventType").asText();
        if (!"QuotaThresholdReached".equals(eventType) && !"QuotaExceeded".equals(eventType)) {
            return;
        }

        UUID eventId = UUID.fromString(envelope.get("eventId").asText());
        JsonNode payload = envelope.get("payload");
        String subscriptionId = payload.hasNonNull("subscriptionId")
                ? payload.get("subscriptionId").asText()
                : envelope.get("aggregateId").asText();

        if (!eventIdempotencyService.tryClaim(eventId, sourceTopic)) {
            log.info("Skipping already-processed event {} ({}) from {}", eventId, eventType, sourceTopic);
            return;
        }

        if ("QuotaThresholdReached".equals(eventType)) {
            mockNotificationDispatcher.dispatchQuotaThresholdWarning(sourceTopic, subscriptionId);
        } else {
            mockNotificationDispatcher.dispatchQuotaExceededWithAddonSuggestion(sourceTopic, subscriptionId);
        }
    }

    private void handle(String sourceTopic, Message<String> message, String requiredEventType) {
        JsonNode envelope;
        try {
            envelope = objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            log.error("Malformed event envelope from {}: {}", sourceTopic, message.getPayload(), ex);
            return;
        }

        String eventType = envelope.get("eventType").asText();
        if (requiredEventType != null && !requiredEventType.equals(eventType)) {
            return;
        }

        UUID eventId = UUID.fromString(envelope.get("eventId").asText());
        String aggregateId = envelope.get("aggregateId").asText();

        if (!eventIdempotencyService.tryClaim(eventId, sourceTopic)) {
            log.info("Skipping already-processed event {} ({}) from {}", eventId, eventType, sourceTopic);
            return;
        }

        dispatch(sourceTopic, eventType, aggregateId, envelope.get("payload"));
    }

    private void dispatch(String sourceTopic, String eventType, String aggregateId, JsonNode payload) {
        TemplateMapping mapping = EVENT_TEMPLATES.get(eventType);
        if (mapping != null) {
            UUID userId = extractUserId(payload, aggregateId, eventType);
            if (userId != null) {
                try {
                    NotificationSendRequest request = new NotificationSendRequest();
                    request.setUserId(userId);
                    request.setTemplateCode(mapping.templateCode());
                    request.setChannelCode(mapping.channelCode());
                    request.setPayload(scalarFieldsOf(payload));
                    notificationService.send(request);
                    return;
                } catch (Exception ex) {
                    log.warn("Templated notification failed for {} (template={}), falling back to mock dispatch",
                            eventType, mapping.templateCode(), ex);
                }
            }
        }
        mockNotificationDispatcher.dispatch(sourceTopic, eventType, aggregateId);
    }

    private UUID extractUserId(JsonNode payload, String aggregateId, String eventType) {
        if ("CustomerRegistered".equals(eventType)) {
            return UUID.fromString(aggregateId);
        }
        if (payload != null && payload.hasNonNull("customerId")) {
            return UUID.fromString(payload.get("customerId").asText());
        }
        return null;
    }

    private Map<String, String> scalarFieldsOf(JsonNode payload) {
        Map<String, String> fields = new HashMap<>();
        if (payload == null) {
            return fields;
        }
        Iterator<Map.Entry<String, JsonNode>> it = payload.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (entry.getValue().isValueNode()) {
                fields.put(entry.getKey(), entry.getValue().asText());
            }
        }
        return fields;
    }

    private record TemplateMapping(String templateCode, String channelCode) {
    }
}
