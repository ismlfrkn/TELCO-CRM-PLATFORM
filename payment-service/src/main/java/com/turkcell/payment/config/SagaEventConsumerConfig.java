package com.turkcell.payment.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.payment.dto.request.PaymentCreateRequest;
import com.turkcell.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Order->Payment bacagi artik CLAUDE.md Bolum 8.2'deki choreography'e gore fonksiyonel Consumer<T>
 * bean'leriyle calisir (senkron OpenFeign zincirinin yerine gecti). Uc yon var:
 *  - orderEvents: OrderCreated'i tuketip odemeyi baslatir (mevcut createPayment idempotency'sini
 *    aynen kullanir - orderId'den turetilen idempotencyKey ile ayni OrderCreated iki kez gelse bile
 *    odeme tekrar islenmez).
 *  - subscriptionEvents: sadece SubscriptionActivationFailed'i dinler, kompansasyon olarak refund
 *    tetikler (payment doğrulama disindaki tum akis asenkron).
 *  - invoiceEvents: FR-24/Bolum 7 "auto-pay" senaryosu - billing-service'in InvoiceGenerated'ini
 *    tuketip faturayi otomatik oder (invoiceId'den turetilen idempotencyKey ile ayni InvoiceGenerated
 *    iki kez gelse bile odeme tekrar islenmez). Payment burada orderId degil invoiceId tasir -
 *    billing-service'in PaymentCompleted consumer'i bu ayrimla siparis odemesini kendisiyle
 *    ilgilendirmeyen bir event olarak eler.
 */
@Configuration
public class SagaEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(SagaEventConsumerConfig.class);
    private static final String PAYMENT_METHOD = "CARD";

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public SagaEventConsumerConfig(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Consumer<Message<String>> orderEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message, "telco.order.events");
            if (envelope == null) {
                return;
            }
            String eventType = envelope.get("eventType").asText();
            if (!"OrderCreated".equals(eventType)) {
                return;
            }

            UUID orderId = UUID.fromString(envelope.get("aggregateId").asText());
            JsonNode payload = envelope.get("payload");
            PaymentCreateRequest request = buildPaymentRequest(orderId, payload);

            paymentService.createPayment("order-" + orderId, request);
        };
    }

    @Bean
    public Consumer<Message<String>> subscriptionEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message, "telco.subscription.events");
            if (envelope == null) {
                return;
            }
            String eventType = envelope.get("eventType").asText();
            if (!"SubscriptionActivationFailed".equals(eventType)) {
                return;
            }

            JsonNode payload = envelope.get("payload");
            UUID orderId = UUID.fromString(payload.get("orderId").asText());
            log.info("Compensating: refunding payment for order {} after subscription activation failure", orderId);
            paymentService.refundByOrderIdIfCompleted(orderId);
        };
    }

    @Bean
    public Consumer<Message<String>> invoiceEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message, "telco.billing.events");
            if (envelope == null) {
                return;
            }
            String eventType = envelope.get("eventType").asText();
            if (!"InvoiceGenerated".equals(eventType)) {
                return;
            }

            UUID invoiceId = UUID.fromString(envelope.get("aggregateId").asText());
            JsonNode payload = envelope.get("payload");
            PaymentCreateRequest request = buildInvoicePaymentRequest(invoiceId, payload);

            paymentService.createPayment("invoice-" + invoiceId, request);
        };
    }

    private PaymentCreateRequest buildInvoicePaymentRequest(UUID invoiceId, JsonNode payload) {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setInvoiceId(invoiceId);
        request.setCustomerId(UUID.fromString(payload.get("customerId").asText()));
        request.setAmount(new BigDecimal(payload.get("grandTotal").asText()));
        request.setCurrency(payload.get("currency").asText());
        request.setMethod(PAYMENT_METHOD);
        return request;
    }

    private PaymentCreateRequest buildPaymentRequest(UUID orderId, JsonNode payload) {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(orderId);
        request.setCustomerId(UUID.fromString(payload.get("customerId").asText()));
        request.setAmount(new BigDecimal(payload.get("totalAmount").asText()));
        request.setCurrency(payload.get("currency").asText());
        request.setMethod(PAYMENT_METHOD);
        tariffCodeOf(payload).ifPresent(request::setTariffCode);
        return request;
    }

    private Optional<String> tariffCodeOf(JsonNode payload) {
        JsonNode items = payload.get("items");
        if (items == null) {
            return Optional.empty();
        }
        for (JsonNode item : items) {
            if ("TARIFF".equals(item.get("productType").asText())) {
                return Optional.of(item.get("productCode").asText());
            }
        }
        return Optional.empty();
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
