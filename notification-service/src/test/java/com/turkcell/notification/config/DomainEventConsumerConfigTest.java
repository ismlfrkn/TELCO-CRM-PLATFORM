package com.turkcell.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.notification.dto.request.NotificationSendRequest;
import com.turkcell.notification.service.EventIdempotencyService;
import com.turkcell.notification.service.MockNotificationDispatcher;
import com.turkcell.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DomainEventConsumerConfigTest {

    private EventIdempotencyService eventIdempotencyService;
    private MockNotificationDispatcher mockNotificationDispatcher;
    private NotificationService notificationService;
    private DomainEventConsumerConfig config;

    @BeforeEach
    void setUp() {
        eventIdempotencyService = mock(EventIdempotencyService.class);
        mockNotificationDispatcher = mock(MockNotificationDispatcher.class);
        notificationService = mock(NotificationService.class);
        config = new DomainEventConsumerConfig(eventIdempotencyService, mockNotificationDispatcher, notificationService, new ObjectMapper());
    }

    private Message<String> envelope(UUID eventId, UUID aggregateId, String eventType) {
        String json = """
                {"eventId":"%s","aggregateType":"Customer","aggregateId":"%s","eventType":"%s","occurredAt":"2026-07-11T00:00:00Z","payload":{}}
                """.formatted(eventId, aggregateId, eventType);
        return MessageBuilder.withPayload(json).build();
    }

    private Message<String> envelopeWithPayload(UUID eventId, UUID aggregateId, String eventType, String payloadJson) {
        String json = """
                {"eventId":"%s","aggregateType":"X","aggregateId":"%s","eventType":"%s","occurredAt":"2026-07-11T00:00:00Z","payload":%s}
                """.formatted(eventId, aggregateId, eventType, payloadJson);
        return MessageBuilder.withPayload(json).build();
    }

    private Message<String> usageEnvelope(UUID eventId, UUID quotaId, UUID subscriptionId, String eventType) {
        String json = """
                {"eventId":"%s","aggregateType":"Quota","aggregateId":"%s","eventType":"%s","occurredAt":"2026-07-11T00:00:00Z",
                "payload":{"id":"%s","subscriptionId":"%s"}}
                """.formatted(eventId, quotaId, eventType, quotaId, subscriptionId);
        return MessageBuilder.withPayload(json).build();
    }

    @Test
    void subscriptionActivatedDispatchesWelcomeSms() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.subscription.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.subscriptionEvents();
        consumer.accept(envelope(eventId, aggregateId, "SubscriptionActivated"));

        verify(mockNotificationDispatcher).dispatch("telco.subscription.events", "SubscriptionActivated", aggregateId.toString());
    }

    @Test
    void subscriptionSuspendedIsIgnoredWithoutDispatchingWelcomeSms() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        Consumer<Message<String>> consumer = config.subscriptionEvents();
        consumer.accept(envelope(eventId, aggregateId, "SubscriptionSuspended"));

        verify(mockNotificationDispatcher, never()).dispatch(anyString(), anyString(), anyString());
        verify(eventIdempotencyService, never()).tryClaim(any(), anyString());
    }

    @Test
    void subscriptionActivatedAlreadyProcessedSkipsDispatch() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.subscription.events"))).thenReturn(false);

        Consumer<Message<String>> consumer = config.subscriptionEvents();
        consumer.accept(envelope(eventId, aggregateId, "SubscriptionActivated"));

        verify(mockNotificationDispatcher, never()).dispatch(anyString(), anyString(), anyString());
    }

    @Test
    void invoiceGeneratedDispatchesInvoiceEmail() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.billing.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.billingEvents();
        consumer.accept(envelope(eventId, aggregateId, "InvoiceGenerated"));

        verify(mockNotificationDispatcher).dispatch("telco.billing.events", "InvoiceGenerated", aggregateId.toString());
    }

    @Test
    void invoicePaidIsIgnoredWithoutDispatchingInvoiceEmail() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        Consumer<Message<String>> consumer = config.billingEvents();
        consumer.accept(envelope(eventId, aggregateId, "InvoicePaid"));

        verify(mockNotificationDispatcher, never()).dispatch(anyString(), anyString(), anyString());
        verify(eventIdempotencyService, never()).tryClaim(any(), anyString());
    }

    @Test
    void dispatchesMockNotificationWhenEventNotYetProcessed() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.customer.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.customerEvents();
        consumer.accept(envelope(eventId, aggregateId, "CustomerCreated"));

        verify(mockNotificationDispatcher).dispatch("telco.customer.events", "CustomerCreated", aggregateId.toString());
    }

    @Test
    void skipsDispatchWhenEventAlreadyProcessed() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.ticket.events"))).thenReturn(false);

        Consumer<Message<String>> consumer = config.ticketEvents();
        consumer.accept(envelope(eventId, aggregateId, "TicketOpened"));

        verify(mockNotificationDispatcher, never()).dispatch(anyString(), anyString(), anyString());
    }

    @Test
    void malformedEnvelopeIsIgnoredWithoutThrowing() {
        Consumer<Message<String>> consumer = config.catalogEvents();

        consumer.accept(MessageBuilder.withPayload("not-json").build());

        verify(mockNotificationDispatcher, never()).dispatch(anyString(), anyString(), anyString());
        verify(eventIdempotencyService, never()).tryClaim(any(), anyString());
    }

    // ---- orderEvents ----

    @Test
    void orderCancelledDispatchesNotification() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.order.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.orderEvents();
        consumer.accept(envelope(eventId, aggregateId, "OrderCancelled"));

        verify(mockNotificationDispatcher).dispatch("telco.order.events", "OrderCancelled", aggregateId.toString());
    }

    @Test
    void orderConfirmedIsIgnoredOnOrderTopic() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        Consumer<Message<String>> consumer = config.orderEvents();
        consumer.accept(envelope(eventId, aggregateId, "OrderConfirmed"));

        verify(mockNotificationDispatcher, never()).dispatch(anyString(), anyString(), anyString());
        verify(eventIdempotencyService, never()).tryClaim(any(), anyString());
    }

    // ---- paymentEvents ----

    @Test
    void paymentFailedDispatchesNotification() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.payment.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.paymentEvents();
        consumer.accept(envelope(eventId, aggregateId, "PaymentFailed"));

        verify(mockNotificationDispatcher).dispatch("telco.payment.events", "PaymentFailed", aggregateId.toString());
    }

    @Test
    void paymentCompletedIsIgnoredOnPaymentTopic() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        Consumer<Message<String>> consumer = config.paymentEvents();
        consumer.accept(envelope(eventId, aggregateId, "PaymentCompleted"));

        verify(mockNotificationDispatcher, never()).dispatch(anyString(), anyString(), anyString());
        verify(eventIdempotencyService, never()).tryClaim(any(), anyString());
    }

    // ---- usageEvents (FR-19) ----

    @Test
    void quotaThresholdReachedDispatchesWarningSms() {
        UUID eventId = UUID.randomUUID();
        UUID quotaId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.usage.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.usageEvents();
        consumer.accept(usageEnvelope(eventId, quotaId, subscriptionId, "QuotaThresholdReached"));

        verify(mockNotificationDispatcher).dispatchQuotaThresholdWarning("telco.usage.events", subscriptionId.toString());
        verify(mockNotificationDispatcher, never()).dispatchQuotaExceededWithAddonSuggestion(anyString(), anyString());
    }

    @Test
    void quotaExceededDispatchesAddonSuggestionSms() {
        UUID eventId = UUID.randomUUID();
        UUID quotaId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.usage.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.usageEvents();
        consumer.accept(usageEnvelope(eventId, quotaId, subscriptionId, "QuotaExceeded"));

        verify(mockNotificationDispatcher).dispatchQuotaExceededWithAddonSuggestion("telco.usage.events", subscriptionId.toString());
        verify(mockNotificationDispatcher, never()).dispatchQuotaThresholdWarning(anyString(), anyString());
    }

    @Test
    void usageRecordedIsIgnoredOnUsageTopic() {
        UUID eventId = UUID.randomUUID();
        UUID quotaId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        Consumer<Message<String>> consumer = config.usageEvents();
        consumer.accept(usageEnvelope(eventId, quotaId, subscriptionId, "UsageRecorded"));

        verify(mockNotificationDispatcher, never()).dispatchQuotaThresholdWarning(anyString(), anyString());
        verify(mockNotificationDispatcher, never()).dispatchQuotaExceededWithAddonSuggestion(anyString(), anyString());
        verify(eventIdempotencyService, never()).tryClaim(any(), anyString());
    }

    @Test
    void quotaThresholdReachedAlreadyProcessedSkipsDispatch() {
        UUID eventId = UUID.randomUUID();
        UUID quotaId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.usage.events"))).thenReturn(false);

        Consumer<Message<String>> consumer = config.usageEvents();
        consumer.accept(usageEnvelope(eventId, quotaId, subscriptionId, "QuotaThresholdReached"));

        verify(mockNotificationDispatcher, never()).dispatchQuotaThresholdWarning(anyString(), anyString());
    }

    @Test
    void malformedUsageEnvelopeIsIgnoredWithoutThrowing() {
        Consumer<Message<String>> consumer = config.usageEvents();

        consumer.accept(MessageBuilder.withPayload("not-json").build());

        verify(mockNotificationDispatcher, never()).dispatchQuotaThresholdWarning(anyString(), anyString());
        verify(mockNotificationDispatcher, never()).dispatchQuotaExceededWithAddonSuggestion(anyString(), anyString());
    }

    // ---- FR-29: sablonlu bildirim (gercek NotificationService.send akisi) ----

    @Test
    void customerRegisteredWithFirstName_sendsTemplatedWelcomeSms() {
        UUID eventId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.customer.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.customerEvents();
        consumer.accept(envelopeWithPayload(eventId, customerId, "CustomerRegistered",
                "{\"firstName\":\"Ali\",\"lastName\":\"Veli\"}"));

        ArgumentCaptor<NotificationSendRequest> captor = ArgumentCaptor.forClass(NotificationSendRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(customerId);
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("CUSTOMER_WELCOME_SMS");
        assertThat(captor.getValue().getChannelCode()).isEqualTo("SMS");
        assertThat(captor.getValue().getPayload()).containsEntry("firstName", "Ali");
        verifyNoInteractions(mockNotificationDispatcher);
    }

    @Test
    void subscriptionActivatedWithCustomerId_sendsTemplatedWelcomeSms() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.subscription.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.subscriptionEvents();
        consumer.accept(envelopeWithPayload(eventId, aggregateId, "SubscriptionActivated",
                "{\"customerId\":\"%s\",\"msisdn\":\"905550000001\",\"tariffCode\":\"STD-100\"}".formatted(customerId)));

        ArgumentCaptor<NotificationSendRequest> captor = ArgumentCaptor.forClass(NotificationSendRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(customerId);
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("WELCOME_SMS");
        assertThat(captor.getValue().getPayload()).containsEntry("msisdn", "905550000001").containsEntry("tariffCode", "STD-100");
        verifyNoInteractions(mockNotificationDispatcher);
    }

    @Test
    void invoiceGeneratedWithCustomerId_sendsTemplatedInvoiceEmail() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.billing.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.billingEvents();
        consumer.accept(envelopeWithPayload(eventId, aggregateId, "InvoiceGenerated",
                "{\"customerId\":\"%s\",\"grandTotal\":179.88,\"currency\":\"TRY\"}".formatted(customerId)));

        ArgumentCaptor<NotificationSendRequest> captor = ArgumentCaptor.forClass(NotificationSendRequest.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(customerId);
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("INVOICE_GENERATED_EMAIL");
        assertThat(captor.getValue().getChannelCode()).isEqualTo("EMAIL");
        verifyNoInteractions(mockNotificationDispatcher);
    }

    @Test
    void whenCustomerIdMissingFromMappedEventPayload_fallsBackToMockDispatch() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.payment.events"))).thenReturn(true);

        Consumer<Message<String>> consumer = config.paymentEvents();
        consumer.accept(envelopeWithPayload(eventId, aggregateId, "PaymentFailed", "{}"));

        verify(mockNotificationDispatcher).dispatch("telco.payment.events", "PaymentFailed", aggregateId.toString());
        verifyNoInteractions(notificationService);
    }

    @Test
    void whenTemplatedSendThrows_fallsBackToMockDispatch() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(eventIdempotencyService.tryClaim(eq(eventId), eq("telco.order.events"))).thenReturn(true);
        when(notificationService.send(any())).thenThrow(new RuntimeException("template not seeded"));

        Consumer<Message<String>> consumer = config.orderEvents();
        consumer.accept(envelopeWithPayload(eventId, aggregateId, "OrderCancelled",
                "{\"customerId\":\"%s\",\"totalAmount\":100.00,\"currency\":\"TRY\"}".formatted(customerId)));

        verify(mockNotificationDispatcher).dispatch("telco.order.events", "OrderCancelled", aggregateId.toString());
    }
}
