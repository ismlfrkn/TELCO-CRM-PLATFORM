package com.turkcell.usage.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.usage.client.ProductCatalogServiceClient;
import com.turkcell.usage.client.TariffClientDto;
import com.turkcell.usage.dto.request.QuotaCreateRequest;
import com.turkcell.usage.exception.DuplicateQuotaException;
import com.turkcell.usage.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * FR-17/18/19: usage-service'in kota takibi yapabilmesi icin, bir abonelik aktive oldugunda
 * o aboneligin Quota kaydinin acilmis olmasi gerekir - CdrEventController/CdrEventConsumerConfig
 * bu kayit yoksa QuotaNotFoundException firlatir. Bu consumer eksikti (bkz. CLAUDE.md Bolum 16):
 * SubscriptionActivated hicbir zaman tuketilmiyordu, yani Quota SADECE POST /api/v1/quotas ile elle
 * acilabiliyordu - kabul kriterindeki "otomatik" akis hic calismiyordu (canli testte yakalandi).
 */
@Configuration
public class SubscriptionEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventConsumerConfig.class);
    private static final String SOURCE_TOPIC = "telco.subscription.events";

    private final QuotaService quotaService;
    private final ProductCatalogServiceClient productCatalogServiceClient;
    private final ObjectMapper objectMapper;

    public SubscriptionEventConsumerConfig(QuotaService quotaService,
                                            ProductCatalogServiceClient productCatalogServiceClient,
                                            ObjectMapper objectMapper) {
        this.quotaService = quotaService;
        this.productCatalogServiceClient = productCatalogServiceClient;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Consumer<Message<String>> subscriptionEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message);
            if (envelope == null) {
                return;
            }

            String eventType = envelope.get("eventType").asText();
            if (!"SubscriptionActivated".equals(eventType)) {
                return;
            }

            try {
                JsonNode payload = envelope.get("payload");
                UUID subscriptionId = UUID.fromString(payload.get("id").asText());
                String tariffCode = payload.get("tariffCode").asText();
                Instant activatedAt = payload.hasNonNull("activatedAt")
                        ? Instant.parse(payload.get("activatedAt").asText())
                        : Instant.now();

                TariffClientDto tariff = productCatalogServiceClient.getTariff(tariffCode);

                LocalDate periodStart = activatedAt.atZone(ZoneOffset.UTC).toLocalDate();
                QuotaCreateRequest request = new QuotaCreateRequest();
                request.setSubscriptionId(subscriptionId);
                request.setPeriodStart(periodStart);
                request.setPeriodEnd(periodStart.plusMonths(1));
                request.setMinutesIncluded(zeroIfNull(tariff.getMinutesIncluded()));
                request.setSmsIncluded(zeroIfNull(tariff.getSmsIncluded()));
                request.setMbIncluded(zeroIfNull(tariff.getDataMbIncluded()));

                quotaService.createQuota(request);
            } catch (DuplicateQuotaException ex) {
                log.info("Quota already exists, ignoring duplicate SubscriptionActivated (at-least-once delivery)");
            } catch (Exception ex) {
                log.error("Failed to process SubscriptionActivated event from {}: {}", SOURCE_TOPIC, message.getPayload(), ex);
            }
        };
    }

    private int zeroIfNull(Integer value) {
        return value != null ? value : 0;
    }

    private JsonNode readEnvelope(Message<String> message) {
        try {
            return objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            log.error("Malformed event envelope from {}: {}", SOURCE_TOPIC, message.getPayload(), ex);
            return null;
        }
    }
}
