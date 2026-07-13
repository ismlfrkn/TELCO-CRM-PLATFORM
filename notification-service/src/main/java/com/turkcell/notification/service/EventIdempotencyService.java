package com.turkcell.notification.service;

import com.turkcell.notification.entity.ProcessedEvent;
import com.turkcell.notification.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * Outbox pattern "en az bir kez teslim" garantisi verdigi icin (bkz. customer/ticket/product-catalog
 * OutboxEventPublisher) ayni event_id ile ayni mesaj birden fazla gelebilir - burada order-service'teki
 * IdempotencyKeyService ile ayni temel yaklasim kullanilir: once kontrol et, sonra BAGIMSIZ bir
 * transaction icinde UNIQUE constraint'e guvenerek insert dene. Iki eszamanli consumer/rebalance ayni
 * event_id'yi ayni anda islemeye calisirsa, DB constraint'i kaybeden tarafi guvenle eler.
 */
@Service
public class EventIdempotencyService {

    private final ProcessedEventRepository processedEventRepository;
    private final TransactionTemplate claimTransactionTemplate;

    public EventIdempotencyService(ProcessedEventRepository processedEventRepository,
                                    PlatformTransactionManager transactionManager) {
        this.processedEventRepository = processedEventRepository;
        this.claimTransactionTemplate = new TransactionTemplate(transactionManager);
        this.claimTransactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * true donerse: bu event_id ilk kez goruluyor, cagiran isleyebilir. false donerse: bu event
     * daha once islenmis (ya da ayni anda baska bir thread/instance tarafindan islenmekte), atlanmali.
     */
    public boolean tryClaim(UUID eventId, String sourceTopic) {
        if (processedEventRepository.existsByEventId(eventId)) {
            return false;
        }

        try {
            claimTransactionTemplate.executeWithoutResult(status -> {
                ProcessedEvent processedEvent = new ProcessedEvent();
                processedEvent.setEventId(eventId);
                processedEvent.setSourceTopic(sourceTopic);
                processedEventRepository.save(processedEvent);
            });
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
