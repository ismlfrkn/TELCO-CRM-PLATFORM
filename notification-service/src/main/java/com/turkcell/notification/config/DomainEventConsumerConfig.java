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

/**
 * customer-service, ticket-service, product-catalog-service, subscription-service ve
 * billing-service'in outbox->Kafka publisher'larinin yazdigi event zarflarini dinleyen fonksiyonel
 * Consumer<T> bean'leri (CLAUDE.md Bolum 10 standardi: spring-cloud-starter-stream-kafka +
 * StreamBridge/Consumer<T>, @KafkaListener kullanilmaz).
 *
 * Her binding'in kendi "in-0" ismi ve application.yml'deki spring.cloud.function.definition listesi
 * araciligiyla bagimsiz olarak tetiklenir - bkz. configs/notification-service/application.yml.
 */
@Configuration
public class DomainEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumerConfig.class);

    /**
     * FR-29: sablonlu bildirim - eventType'i, o event icin V4 migration'da seed edilen sablonun
     * kodu+kanaliyla eslestirir. Burada olmayan event tipleri (TicketOpened, TariffCreated vb.)
     * mockNotificationDispatcher'in jenerik log'una duser - her event turu icin sablon uretmek bu
     * calismanin kapsami disinda tutuldu, en azindan CLAUDE.md'nin acikca adlandirdigi akislar
     * (welcome SMS, fatura e-postasi) gercek icerik uretiyor.
     */
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

    /**
     * telco.subscription.events topic'inde SubscriptionActivated disinda SubscriptionSuspended/
     * Terminated/ActivationFailed gibi event'ler de akar; welcome SMS sadece aktivasyonda gonderilir,
     * digerleri sessizce yok sayilir (CLAUDE.md 14.1 senaryosu adim 6).
     */
    @Bean
    public Consumer<Message<String>> subscriptionEvents() {
        return message -> handle("telco.subscription.events", message, "SubscriptionActivated");
    }

    /**
     * telco.billing.events topic'inde InvoiceGenerated disinda InvoicePaid/InvoiceOverdue de akar;
     * fatura e-postasi sadece fatura kesildiginde gonderilir (Bolum 8.1 "Invoice -> Notification").
     */
    @Bean
    public Consumer<Message<String>> billingEvents() {
        return message -> handle("telco.billing.events", message, "InvoiceGenerated");
    }

    /**
     * telco.order.events topic'inde OrderCreated/OrderConfirmed de akar; musteriye sadece iptal
     * bilgilendirmesi gonderilir, digerleri sessizce yok sayilir.
     */
    @Bean
    public Consumer<Message<String>> orderEvents() {
        return message -> handle("telco.order.events", message, "OrderCancelled");
    }

    /**
     * telco.payment.events topic'inde PaymentCompleted/PaymentRefunded de akar; musteriye sadece
     * basarisiz odeme bilgilendirmesi gonderilir, digerleri sessizce yok sayilir.
     */
    @Bean
    public Consumer<Message<String>> paymentEvents() {
        return message -> handle("telco.payment.events", message, "PaymentFailed");
    }

    /**
     * FR-19: telco.usage.events topic'inde UsageRecorded/UsageAggregated da akar; sadece
     * QuotaThresholdReached (%80 uyari) ve QuotaExceeded (%100 + ek paket onerisi) icin, birbirinden
     * FARKLI metinli mock SMS'ler uretilir - bu yuzden genel handle(...) yerine kendi metodu var
     * (CLAUDE.md 14.1 Senaryo 3).
     */
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

    /**
     * requiredEventType null ise topic'teki her event'e bildirim gonderilir (customer/ticket/catalog
     * topic'lerinin hepsi zaten bildirim gonderilmesi gereken event'lerden olusuyor); dolu ise sadece
     * o event tipinde gonderilir, digerleri sessizce yok sayilir (subscription/billing gibi ayni
     * topic'te bildirim gerektirmeyen event'lerin de aktigi durumlarda).
     */
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

    /**
     * EVENT_TEMPLATES'te kayitli bir event turu icin, once gercek sablon+kanal akisini
     * (NotificationService.send) dener - basarili olursa DB'de gercek bir Notification kaydi olusur
     * ve NotificationDispatched kendi outbox'i uzerinden yayinlanir. Sablon/kanal bulunamazsa (henuz
     * seed edilmemis) ya da userId payload'dan cikarilamazsa, eskisi gibi mock log'a duser - boylece
     * bildirim sinyali hicbir durumda tamamen kaybolmaz.
     */
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

    /**
     * CustomerRegistered'da aggregateId zaten customerId'dir (aggregateType=Customer); diger
     * eslesmis event'lerin (SubscriptionActivated/InvoiceGenerated/OrderCancelled/PaymentFailed)
     * hepsi payload'unda customerId tasir (Grup 3/FR-22 calismalarinda saga korelasyonu icin
     * eklendi).
     */
    private UUID extractUserId(JsonNode payload, String aggregateId, String eventType) {
        if ("CustomerRegistered".equals(eventType)) {
            return UUID.fromString(aggregateId);
        }
        if (payload != null && payload.hasNonNull("customerId")) {
            return UUID.fromString(payload.get("customerId").asText());
        }
        return null;
    }

    /**
     * Sablondaki {{alan}} yer tutucularini doldurmak icin, payload'daki tum skaler (nesne/dizi
     * olmayan) alanlari oldugu gibi Map'e aktarir - her event turu icin ayri bir alan listesi
     * tutmaya gerek birakmaz.
     */
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
