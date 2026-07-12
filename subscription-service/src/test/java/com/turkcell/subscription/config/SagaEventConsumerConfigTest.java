package com.turkcell.subscription.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.subscription.dto.request.SubscriptionCreateRequest;
import com.turkcell.subscription.exception.NoAvailableMsisdnException;
import com.turkcell.subscription.service.OutboxEventService;
import com.turkcell.subscription.service.SubscriptionService;
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
import static org.mockito.Mockito.when;

class SagaEventConsumerConfigTest {

    private SubscriptionService subscriptionService;
    private OutboxEventService outboxEventService;
    private SagaEventConsumerConfig config;

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        outboxEventService = mock(OutboxEventService.class);
        config = new SagaEventConsumerConfig(subscriptionService, outboxEventService, new ObjectMapper());
    }

    private String paymentCompletedJson(UUID orderId, UUID customerId, String tariffCode) {
        String tariffField = tariffCode == null ? "null" : "\"" + tariffCode + "\"";
        return """
                {"eventId":"%s","aggregateType":"Payment","aggregateId":"%s","eventType":"PaymentCompleted","occurredAt":"2026-07-11T00:00:00Z",
                "payload":{"id":"%s","orderId":"%s","customerId":"%s","tariffCode":%s,"amount":99.90,"currency":"TRY","status":"COMPLETED"}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), orderId, customerId, tariffField);
    }

    @Test
    void paymentCompletedWithTariffActivatesSubscription() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Consumer<Message<String>> consumer = config.paymentEvents();

        consumer.accept(MessageBuilder.withPayload(paymentCompletedJson(orderId, customerId, "TARIFF-STD")).build());

        ArgumentCaptor<SubscriptionCreateRequest> captor = ArgumentCaptor.forClass(SubscriptionCreateRequest.class);
        verify(subscriptionService).createSubscription(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getCustomerId()).isEqualTo(customerId);
        assertThat(captor.getValue().getTariffCode()).isEqualTo("TARIFF-STD");
    }

    @Test
    void paymentCompletedWithoutTariffIsIgnored() {
        Consumer<Message<String>> consumer = config.paymentEvents();

        consumer.accept(MessageBuilder.withPayload(
                paymentCompletedJson(UUID.randomUUID(), UUID.randomUUID(), null)).build());

        verify(subscriptionService, never()).createSubscription(any());
    }

    @Test
    void nonPaymentCompletedEventIsIgnored() {
        String json = """
                {"eventId":"%s","aggregateType":"Payment","aggregateId":"%s","eventType":"PaymentFailed","occurredAt":"2026-07-11T00:00:00Z","payload":{}}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        config.paymentEvents().accept(MessageBuilder.withPayload(json).build());

        verify(subscriptionService, never()).createSubscription(any());
    }

    @Test
    void activationFailurePublishesSubscriptionActivationFailedEvent() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(subscriptionService.createSubscription(any()))
                .thenThrow(new NoAvailableMsisdnException("MSISDN pool is exhausted"));

        config.paymentEvents().accept(MessageBuilder.withPayload(
                paymentCompletedJson(orderId, customerId, "TARIFF-STD")).build());

        verify(outboxEventService).publish(eq("Subscription"), eq(orderId), eq("SubscriptionActivationFailed"), any());
    }

    @Test
    void malformedEnvelopeIsIgnoredWithoutThrowing() {
        config.paymentEvents().accept(MessageBuilder.withPayload("not-json").build());

        verify(subscriptionService, never()).createSubscription(any());
        verify(outboxEventService, never()).publish(anyString(), any(), anyString(), any());
    }
}
