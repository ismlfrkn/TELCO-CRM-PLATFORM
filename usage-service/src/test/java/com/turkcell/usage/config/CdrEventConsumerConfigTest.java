package com.turkcell.usage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turkcell.usage.dto.request.CdrEventIngestRequest;
import com.turkcell.usage.service.CdrEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CdrEventConsumerConfigTest {

    private CdrEventService cdrEventService;
    private CdrEventConsumerConfig config;

    @BeforeEach
    void setUp() {
        cdrEventService = mock(CdrEventService.class);
        config = new CdrEventConsumerConfig(cdrEventService, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    private String envelope(String eventType, String payloadJson) {
        return """
                {"eventId":"%s","aggregateType":"Cdr","aggregateId":"%s","eventType":"%s","occurredAt":"2026-07-12T00:00:00Z",
                "payload":%s}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), eventType, payloadJson);
    }

    private String cdrPayload(UUID subscriptionId, String externalCdrId) {
        return """
                {"externalCdrId":"%s","subscriptionId":"%s","msisdn":"905550000001","cdrType":"VOICE",
                "startTime":"%s","durationSeconds":120}
                """.formatted(externalCdrId, subscriptionId, Instant.now());
    }

    @Test
    void cdrRecordedIngestsRequestViaCdrEventService() {
        UUID subscriptionId = UUID.randomUUID();
        Consumer<Message<String>> consumer = config.cdrEvents();

        consumer.accept(MessageBuilder.withPayload(envelope("CdrRecorded", cdrPayload(subscriptionId, "CDR-EXT-1"))).build());

        ArgumentCaptor<CdrEventIngestRequest> captor = ArgumentCaptor.forClass(CdrEventIngestRequest.class);
        verify(cdrEventService).ingest(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getExternalCdrId()).isEqualTo("CDR-EXT-1");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getSubscriptionId()).isEqualTo(subscriptionId);
    }

    @Test
    void otherEventTypesAreIgnored() {
        Consumer<Message<String>> consumer = config.cdrEvents();

        consumer.accept(MessageBuilder.withPayload(envelope("CdrCorrected", cdrPayload(UUID.randomUUID(), "CDR-EXT-2"))).build());

        verifyNoInteractions(cdrEventService);
    }

    @Test
    void malformedEnvelopeIsIgnoredWithoutThrowing() {
        Consumer<Message<String>> consumer = config.cdrEvents();

        consumer.accept(MessageBuilder.withPayload("not-json").build());

        verifyNoInteractions(cdrEventService);
    }

    @Test
    void exceptionFromIngestIsSwallowedWithoutRethrowing() {
        UUID subscriptionId = UUID.randomUUID();
        org.mockito.Mockito.when(cdrEventService.ingest(any())).thenThrow(new RuntimeException("db down"));
        Consumer<Message<String>> consumer = config.cdrEvents();

        consumer.accept(MessageBuilder.withPayload(envelope("CdrRecorded", cdrPayload(subscriptionId, "CDR-EXT-3"))).build());

        verify(cdrEventService).ingest(any());
    }
}
