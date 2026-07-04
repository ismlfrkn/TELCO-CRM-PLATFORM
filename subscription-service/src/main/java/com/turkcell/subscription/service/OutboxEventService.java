package com.turkcell.subscription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.subscription.entity.OutboxEvent;
import com.turkcell.subscription.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Domain event'leri outbox tablosuna yazar. Cagiran servis metodu zaten @Transactional oldugu icin
 * bu satir, is durumundaki degisiklikle (ornegin Subscription.save) AYNI DB transaction'inda commit
 * olur - boylece "durum degisti ama event kayboldu" ya da tam tersi asla olmaz (outbox pattern).
 * Bu satirlari gercekten Kafka'ya basacak bir publisher worker henuz yok; bu servis sadece "yazma"
 * tarafini dogru sekilde kapatir.
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
