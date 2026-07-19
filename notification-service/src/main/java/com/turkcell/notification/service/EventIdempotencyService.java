package com.turkcell.notification.service;

import com.turkcell.notification.entity.ProcessedEvent;
import com.turkcell.notification.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

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
