package com.turkcell.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.notification.entity.OutboxEvent;
import com.turkcell.notification.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Domain event'leri outbox tablosuna yazar. Cagiran servis metodu zaten @Transactional oldugu icin
 * bu satir, is durumundaki degisiklikle AYNI DB transaction'inda commit olur (outbox pattern).
 * Yazilan satirlar OutboxEventPublisher tarafindan periyodik taranip Kafka'ya gonderilir.
 */
@Service
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publish(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(toJson(payload));
        event.setStatus(OutboxEvent.STATUS_PENDING);
        outboxEventRepository.save(event);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
