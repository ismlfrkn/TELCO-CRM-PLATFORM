package com.turkcell.billing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.billing.dto.request.BillCycleCreateRequest;
import com.turkcell.billing.entity.UsageAggregate;
import com.turkcell.billing.exception.DuplicateBillCycleException;
import com.turkcell.billing.exception.InvalidInvoiceStateException;
import com.turkcell.billing.repository.UsageAggregateRepository;
import com.turkcell.billing.service.BillCycleService;
import com.turkcell.billing.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BillingEventConsumerConfigTest {

    private BillCycleService billCycleService;
    private InvoiceService invoiceService;
    private UsageAggregateRepository usageAggregateRepository;
    private BillingEventConsumerConfig config;

    @BeforeEach
    void setUp() {
        billCycleService = mock(BillCycleService.class);
        invoiceService = mock(InvoiceService.class);
        usageAggregateRepository = mock(UsageAggregateRepository.class);
        config = new BillingEventConsumerConfig(billCycleService, invoiceService, usageAggregateRepository, new ObjectMapper());
    }

    private Message<String> envelope(UUID eventId, String eventType, String payloadJson) {
        String json = """
                {"eventId":"%s","aggregateType":"X","aggregateId":"%s","eventType":"%s","occurredAt":"2026-07-12T00:00:00Z",
                "payload":%s}
                """.formatted(eventId, UUID.randomUUID(), eventType, payloadJson);
        return MessageBuilder.withPayload(json).build();
    }

    // ---- subscriptionEvents ----

    @Test
    void subscriptionActivatedCreatesBillCycle() {
        UUID customerId = UUID.randomUUID();
        String payload = """
                {"customerId":"%s","activatedAt":"2026-07-15T10:00:00Z"}
                """.formatted(customerId);

        Consumer<Message<String>> consumer = config.subscriptionEvents();
        consumer.accept(envelope(UUID.randomUUID(), "SubscriptionActivated", payload));

        ArgumentCaptor<BillCycleCreateRequest> captor = ArgumentCaptor.forClass(BillCycleCreateRequest.class);
        verify(billCycleService).createBillCycle(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(customerId);
        assertThat(captor.getValue().getDayOfMonth()).isEqualTo(15);
    }

    @Test
    void subscriptionActivatedClampsDayOfMonthAbove28() {
        UUID customerId = UUID.randomUUID();
        String payload = """
                {"customerId":"%s","activatedAt":"2026-07-31T10:00:00Z"}
                """.formatted(customerId);

        config.subscriptionEvents().accept(envelope(UUID.randomUUID(), "SubscriptionActivated", payload));

        ArgumentCaptor<BillCycleCreateRequest> captor = ArgumentCaptor.forClass(BillCycleCreateRequest.class);
        verify(billCycleService).createBillCycle(captor.capture());
        assertThat(captor.getValue().getDayOfMonth()).isEqualTo(28);
    }

    @Test
    void subscriptionActivatedWhenBillCycleAlreadyExists_isIgnoredWithoutThrowing() {
        UUID customerId = UUID.randomUUID();
        String payload = """
                {"customerId":"%s","activatedAt":"2026-07-15T10:00:00Z"}
                """.formatted(customerId);
        when(billCycleService.createBillCycle(any())).thenThrow(new DuplicateBillCycleException("already exists"));

        config.subscriptionEvents().accept(envelope(UUID.randomUUID(), "SubscriptionActivated", payload));
        // exception yutulur, test'in kendisi hic firlatilmadan tamamlanmasini dogrular
    }

    @Test
    void subscriptionSuspendedIsIgnored() {
        String payload = """
                {"customerId":"%s"}
                """.formatted(UUID.randomUUID());

        config.subscriptionEvents().accept(envelope(UUID.randomUUID(), "SubscriptionSuspended", payload));

        verifyNoInteractions(billCycleService);
    }

    // ---- paymentEvents ----

    @Test
    void paymentCompletedWithInvoiceIdMarksInvoicePaid() {
        UUID invoiceId = UUID.randomUUID();
        String payload = """
                {"invoiceId":"%s","orderId":null}
                """.formatted(invoiceId);

        config.paymentEvents().accept(envelope(UUID.randomUUID(), "PaymentCompleted", payload));

        verify(invoiceService).markPaid(invoiceId);
    }

    @Test
    void paymentCompletedWithoutInvoiceIdIsIgnored() {
        String payload = """
                {"orderId":"%s","invoiceId":null}
                """.formatted(UUID.randomUUID());

        config.paymentEvents().accept(envelope(UUID.randomUUID(), "PaymentCompleted", payload));

        verifyNoInteractions(invoiceService);
    }

    @Test
    void paymentCompletedForAlreadyPaidInvoice_isIgnoredWithoutThrowing() {
        UUID invoiceId = UUID.randomUUID();
        String payload = """
                {"invoiceId":"%s"}
                """.formatted(invoiceId);
        doThrow(new InvalidInvoiceStateException("already PAID")).when(invoiceService).markPaid(invoiceId);

        config.paymentEvents().accept(envelope(UUID.randomUUID(), "PaymentCompleted", payload));
        // exception yutulur
    }

    @Test
    void paymentFailedIsIgnored() {
        String payload = """
                {"invoiceId":"%s"}
                """.formatted(UUID.randomUUID());

        config.paymentEvents().accept(envelope(UUID.randomUUID(), "PaymentFailed", payload));

        verifyNoInteractions(invoiceService);
    }

    // ---- usageEvents ----

    @Test
    void usageAggregatedIsPersisted() {
        UUID eventId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        String payload = """
                {"subscriptionId":"%s","quotaId":"%s","cdrType":"VOICE","overageQuantity":5,
                "periodStart":"2026-07-01","periodEnd":"2026-07-31"}
                """.formatted(subscriptionId, UUID.randomUUID());

        config.usageEvents().accept(envelope(eventId, "UsageAggregated", payload));

        ArgumentCaptor<UsageAggregate> captor = ArgumentCaptor.forClass(UsageAggregate.class);
        verify(usageAggregateRepository).save(captor.capture());
        assertThat(captor.getValue().getSourceEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(captor.getValue().getOverageQuantity()).isEqualByComparingTo("5");
    }

    @Test
    void usageAggregatedDuplicateDelivery_isIgnoredWithoutThrowing() {
        when(usageAggregateRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        String payload = """
                {"subscriptionId":"%s","cdrType":"VOICE","overageQuantity":5}
                """.formatted(UUID.randomUUID());

        config.usageEvents().accept(envelope(UUID.randomUUID(), "UsageAggregated", payload));
        // exception yutulur
    }

    @Test
    void quotaThresholdReachedIsIgnored() {
        String payload = """
                {"subscriptionId":"%s"}
                """.formatted(UUID.randomUUID());

        config.usageEvents().accept(envelope(UUID.randomUUID(), "QuotaThresholdReached", payload));

        verifyNoInteractions(usageAggregateRepository);
    }

    @Test
    void malformedEnvelopeIsIgnoredWithoutThrowing() {
        config.subscriptionEvents().accept(MessageBuilder.withPayload("not-json").build());

        verifyNoInteractions(billCycleService);
    }
}
