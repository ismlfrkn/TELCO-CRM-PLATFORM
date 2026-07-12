package com.turkcell.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.payment.dto.request.PaymentCreateRequest;
import com.turkcell.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SagaEventConsumerConfigTest {

    private PaymentService paymentService;
    private SagaEventConsumerConfig config;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        config = new SagaEventConsumerConfig(paymentService, new ObjectMapper());
    }

    private Message<String> orderCreatedEnvelope(UUID orderId, UUID customerId) {
        String json = """
                {"eventId":"%s","aggregateType":"Order","aggregateId":"%s","eventType":"OrderCreated","occurredAt":"2026-07-11T00:00:00Z",
                "payload":{"id":"%s","customerId":"%s","status":"PENDING_PAYMENT","totalAmount":99.90,"currency":"TRY",
                "items":[{"productCode":"TARIFF-STD","productType":"TARIFF","quantity":1,"unitPrice":99.90}]}}
                """.formatted(UUID.randomUUID(), orderId, orderId, customerId);
        return MessageBuilder.withPayload(json).build();
    }

    @Test
    void orderCreatedTriggersPaymentWithOrderCorrelationAndTariffCode() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Consumer<Message<String>> consumer = config.orderEvents();
        consumer.accept(orderCreatedEnvelope(orderId, customerId));

        ArgumentCaptor<PaymentCreateRequest> captor = ArgumentCaptor.forClass(PaymentCreateRequest.class);
        verify(paymentService).createPayment(eq("order-" + orderId), captor.capture());

        PaymentCreateRequest request = captor.getValue();
        assertThat(request.getOrderId()).isEqualTo(orderId);
        assertThat(request.getCustomerId()).isEqualTo(customerId);
        assertThat(request.getTariffCode()).isEqualTo("TARIFF-STD");
        assertThat(request.getAmount()).isEqualByComparingTo(new BigDecimal("99.90"));
        assertThat(request.getCurrency()).isEqualTo("TRY");
        assertThat(request.getMethod()).isEqualTo("CARD");
    }

    @Test
    void nonOrderCreatedEventOnOrderTopicIsIgnored() {
        String json = """
                {"eventId":"%s","aggregateType":"Order","aggregateId":"%s","eventType":"OrderCancelled","occurredAt":"2026-07-11T00:00:00Z","payload":{}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        config.orderEvents().accept(MessageBuilder.withPayload(json).build());

        verify(paymentService, never()).createPayment(anyString(), any());
    }

    @Test
    void subscriptionActivationFailedTriggersRefundCompensation() {
        UUID orderId = UUID.randomUUID();
        String json = """
                {"eventId":"%s","aggregateType":"Subscription","aggregateId":"%s","eventType":"SubscriptionActivationFailed","occurredAt":"2026-07-11T00:00:00Z",
                "payload":{"orderId":"%s","reason":"MSISDN pool exhausted"}}
                """.formatted(UUID.randomUUID(), orderId, orderId);

        config.subscriptionEvents().accept(MessageBuilder.withPayload(json).build());

        verify(paymentService).refundByOrderIdIfCompleted(orderId);
    }

    @Test
    void subscriptionActivatedIsIgnoredByPaymentService() {
        String json = """
                {"eventId":"%s","aggregateType":"Subscription","aggregateId":"%s","eventType":"SubscriptionActivated","occurredAt":"2026-07-11T00:00:00Z","payload":{"orderId":"%s"}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        config.subscriptionEvents().accept(MessageBuilder.withPayload(json).build());

        verify(paymentService, never()).refundByOrderIdIfCompleted(any());
    }

    @Test
    void malformedEnvelopeIsIgnoredWithoutThrowing() {
        config.orderEvents().accept(MessageBuilder.withPayload("not-json").build());

        verify(paymentService, never()).createPayment(anyString(), any());
    }

    private Message<String> invoiceGeneratedEnvelope(UUID invoiceId, UUID customerId) {
        String json = """
                {"eventId":"%s","aggregateType":"Invoice","aggregateId":"%s","eventType":"InvoiceGenerated","occurredAt":"2026-07-11T00:00:00Z",
                "payload":{"id":"%s","customerId":"%s","grandTotal":121.00,"currency":"TRY","status":"PENDING"}}
                """.formatted(UUID.randomUUID(), invoiceId, invoiceId, customerId);
        return MessageBuilder.withPayload(json).build();
    }

    @Test
    void invoiceGeneratedTriggersAutoPayWithInvoiceCorrelation() {
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Consumer<Message<String>> consumer = config.invoiceEvents();
        consumer.accept(invoiceGeneratedEnvelope(invoiceId, customerId));

        ArgumentCaptor<PaymentCreateRequest> captor = ArgumentCaptor.forClass(PaymentCreateRequest.class);
        verify(paymentService).createPayment(eq("invoice-" + invoiceId), captor.capture());

        PaymentCreateRequest request = captor.getValue();
        assertThat(request.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(request.getCustomerId()).isEqualTo(customerId);
        assertThat(request.getOrderId()).isNull();
        assertThat(request.getAmount()).isEqualByComparingTo(new BigDecimal("121.00"));
        assertThat(request.getCurrency()).isEqualTo("TRY");
        assertThat(request.getMethod()).isEqualTo("CARD");
    }

    @Test
    void nonInvoiceGeneratedEventOnBillingTopicIsIgnored() {
        String json = """
                {"eventId":"%s","aggregateType":"Invoice","aggregateId":"%s","eventType":"InvoicePaid","occurredAt":"2026-07-11T00:00:00Z","payload":{}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        config.invoiceEvents().accept(MessageBuilder.withPayload(json).build());

        verify(paymentService, never()).createPayment(anyString(), any());
    }

    @Test
    void malformedInvoiceEnvelopeIsIgnoredWithoutThrowing() {
        config.invoiceEvents().accept(MessageBuilder.withPayload("not-json").build());

        verify(paymentService, never()).createPayment(anyString(), any());
    }
}
