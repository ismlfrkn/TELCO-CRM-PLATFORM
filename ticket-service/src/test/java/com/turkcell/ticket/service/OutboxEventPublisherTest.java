package com.turkcell.ticket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.ticket.entity.OutboxEvent;
import com.turkcell.ticket.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventPublisherTest {

    private OutboxEventRepository outboxEventRepository;
    private StreamBridge streamBridge;
    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        streamBridge = mock(StreamBridge.class);
        publisher = new OutboxEventPublisher(outboxEventRepository, streamBridge, new ObjectMapper(), 20);
    }

    private OutboxEvent pendingEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("Ticket");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("TicketOpened");
        event.setPayload("{\"id\":\"abc\"}");
        event.setStatus(OutboxEvent.STATUS_PENDING);
        event.setRetryCount(0);
        event.setCreatedAt(Instant.now());
        return event;
    }

    private void mockPendingBatch(OutboxEvent event) {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxEvent.STATUS_PENDING), any(PageRequest.class)))
                .thenReturn(List.of(event));
    }

    @Test
    void marksEventPublishedWhenSendSucceeds() {
        OutboxEvent event = pendingEvent();
        mockPendingBatch(event);
        when(streamBridge.send(eq("ticketEvents-out-0"), any(Message.class))).thenReturn(true);

        publisher.pollAndPublish();

        OutboxEvent saved = capturedSave();
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.STATUS_PUBLISHED);
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    @Test
    void incrementsRetryCountAndKeepsPendingWhenSendReturnsFalse() {
        OutboxEvent event = pendingEvent();
        mockPendingBatch(event);
        when(streamBridge.send(eq("ticketEvents-out-0"), any(Message.class))).thenReturn(false);

        publisher.pollAndPublish();

        OutboxEvent saved = capturedSave();
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.STATUS_PENDING);
        assertThat(saved.getRetryCount()).isEqualTo(1);
        assertThat(saved.getLastError()).isNotNull();
    }

    @Test
    void incrementsRetryCountWhenSendThrows() {
        OutboxEvent event = pendingEvent();
        mockPendingBatch(event);
        when(streamBridge.send(eq("ticketEvents-out-0"), any(Message.class)))
                .thenThrow(new RuntimeException("broker unreachable"));

        publisher.pollAndPublish();

        OutboxEvent saved = capturedSave();
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.STATUS_PENDING);
        assertThat(saved.getRetryCount()).isEqualTo(1);
        assertThat(saved.getLastError()).isEqualTo("broker unreachable");
    }

    @Test
    void marksEventFailedAfterMaxRetryCount() {
        OutboxEvent event = pendingEvent();
        event.setRetryCount(9);
        mockPendingBatch(event);
        when(streamBridge.send(eq("ticketEvents-out-0"), any(Message.class))).thenReturn(false);

        publisher.pollAndPublish();

        OutboxEvent saved = capturedSave();
        assertThat(saved.getRetryCount()).isEqualTo(10);
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.STATUS_FAILED);
    }

    private OutboxEvent capturedSave() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        return captor.getValue();
    }
}
