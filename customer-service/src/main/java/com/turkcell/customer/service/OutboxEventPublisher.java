package com.turkcell.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.customer.entity.OutboxEvent;
import com.turkcell.customer.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private static final String BINDING_NAME = "customerEvents-out-0";
    private static final int MAX_RETRY_COUNT = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, StreamBridge streamBridge,
                                 ObjectMapper objectMapper,
                                 @Value("${outbox.publisher.batch-size:20}") int batchSize) {
        this.outboxEventRepository = outboxEventRepository;
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.poll-interval-ms:5000}")
    public void pollAndPublish() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEvent.STATUS_PENDING, PageRequest.of(0, batchSize));

        for (OutboxEvent event : pending) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {

            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(buildEnvelope(event))
                    .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString().getBytes(StandardCharsets.UTF_8))
                    .build();

            boolean sent = streamBridge.send(BINDING_NAME, message);
            if (sent) {
                markPublished(event);
            } else {
                markFailed(event, "streamBridge.send returned false");
            }
        } catch (Exception ex) {
            markFailed(event, ex.getMessage());
        }
    }

    private Map<String, Object> buildEnvelope(OutboxEvent event) throws Exception {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.getId().toString());
        envelope.put("aggregateType", event.getAggregateType());
        envelope.put("aggregateId", event.getAggregateId().toString());
        envelope.put("eventType", event.getEventType());
        envelope.put("occurredAt", event.getCreatedAt().toString());
        envelope.put("payload", objectMapper.readTree(event.getPayload()));
        return envelope;
    }

    void markPublished(OutboxEvent event) {
        event.setStatus(OutboxEvent.STATUS_PUBLISHED);
        event.setPublishedAt(Instant.now());
        outboxEventRepository.save(event);
    }

    void markFailed(OutboxEvent event, String errorMessage) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(errorMessage);
        if (event.getRetryCount() >= MAX_RETRY_COUNT) {
            event.setStatus(OutboxEvent.STATUS_FAILED);
            log.error("Outbox event {} exceeded max retry count ({}), marking FAILED", event.getId(), MAX_RETRY_COUNT);
        }
        outboxEventRepository.save(event);
        log.warn("Failed to publish outbox event {} (attempt {}): {}", event.getId(), event.getRetryCount(), errorMessage);
    }
}
