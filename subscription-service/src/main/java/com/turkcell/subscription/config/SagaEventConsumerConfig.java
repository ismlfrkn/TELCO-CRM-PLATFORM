package com.turkcell.subscription.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.subscription.dto.request.SubscriptionCreateRequest;
import com.turkcell.subscription.service.OutboxEventService;
import com.turkcell.subscription.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Order->Subscription aktivasyonu CLAUDE.md Bolum 8.2'deki choreography'e gore fonksiyonel
 * Consumer<T> bean'i ile calisir: sadece PaymentCompleted'e tepki verir (PaymentFailed order-service'in
 * dogrudan ilgilendigi bir konu, subscription-service'i ilgilendirmez). Aktivasyon basarisiz olursa
 * (orn. MSISDN havuzu tukendi) SubscriptionActivationFailed yayinlanir - bu hem payment-service'teki
 * refund kompansasyonunu hem de order-service'teki OrderCancelled'i bagimsiz olarak tetikler.
 */
@Configuration
public class SagaEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(SagaEventConsumerConfig.class);
    private static final String AGGREGATE_TYPE = "Subscription";

    private final SubscriptionService subscriptionService;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;

    public SagaEventConsumerConfig(SubscriptionService subscriptionService, OutboxEventService outboxEventService,
                                    ObjectMapper objectMapper) {
        this.subscriptionService = subscriptionService;
        this.outboxEventService = outboxEventService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Consumer<Message<String>> paymentEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message);
            if (envelope == null) {
                return;
            }
            String eventType = envelope.get("eventType").asText();
            if (!"PaymentCompleted".equals(eventType)) {
                return;
            }

            JsonNode payload = envelope.get("payload");
            UUID orderId = UUID.fromString(payload.get("orderId").asText());
            UUID customerId = UUID.fromString(payload.get("customerId").asText());
            String tariffCode = payload.hasNonNull("tariffCode") ? payload.get("tariffCode").asText() : null;

            if (tariffCode == null) {
                log.info("PaymentCompleted for order {} has no tariff item, no subscription to activate", orderId);
                return;
            }

            SubscriptionCreateRequest request = new SubscriptionCreateRequest();
            request.setOrderId(orderId);
            request.setCustomerId(customerId);
            request.setTariffCode(tariffCode);

            try {
                subscriptionService.createSubscription(request);
            } catch (Exception ex) {
                log.warn("Subscription activation failed for order {}, publishing SubscriptionActivationFailed",
                        orderId, ex);
                publishActivationFailed(orderId, customerId, tariffCode, ex.getMessage());
            }
        };
    }

    private void publishActivationFailed(UUID orderId, UUID customerId, String tariffCode, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("customerId", customerId.toString());
        payload.put("tariffCode", tariffCode);
        payload.put("reason", reason);
        // aggregateId olarak orderId kullanilir - bu basarisizlikta olusmus bir Subscription yok,
        // ama tuketicilerin (payment-service, order-service) korele edecegi tek ortak kimlik orderId.
        outboxEventService.publish(AGGREGATE_TYPE, orderId, "SubscriptionActivationFailed", payload);
    }

    private JsonNode readEnvelope(Message<String> message) {
        try {
            return objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            log.error("Malformed event envelope from telco.payment.events: {}", message.getPayload(), ex);
            return null;
        }
    }
}
