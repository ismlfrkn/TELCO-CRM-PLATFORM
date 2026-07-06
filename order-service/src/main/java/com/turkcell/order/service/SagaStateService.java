package com.turkcell.order.service;

import com.turkcell.order.entity.SagaState;
import com.turkcell.order.repository.SagaStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SagaStateService {

    private final SagaStateRepository sagaStateRepository;

    public SagaStateService(SagaStateRepository sagaStateRepository) {
        this.sagaStateRepository = sagaStateRepository;
    }

    @Transactional
    public void initialize(UUID orderId, String step) {
        SagaState state = new SagaState();
        state.setOrderId(orderId);
        state.setCurrentStep(step);
        state.setLastUpdated(Instant.now());
        sagaStateRepository.save(state);
    }

    @Transactional
    public void advance(UUID orderId, String step) {
        SagaState state = sagaStateRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("Saga state not found for order: " + orderId));
        state.setCurrentStep(step);
        state.setLastUpdated(Instant.now());
        sagaStateRepository.save(state);
    }
}
