package com.turkcell.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.order.entity.IdempotencyKey;
import com.turkcell.order.exception.IdempotencyKeyReusedException;
import com.turkcell.order.exception.OrderProcessingInProgressException;
import com.turkcell.order.repository.IdempotencyKeyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyKeyService {

    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate claimTransactionTemplate;

    public IdempotencyKeyService(IdempotencyKeyRepository idempotencyKeyRepository, ObjectMapper objectMapper,
                                  PlatformTransactionManager transactionManager) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
        this.claimTransactionTemplate = new TransactionTemplate(transactionManager);
        this.claimTransactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    public String hash(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash request", ex);
        }
    }

    public <T> Optional<T> tryClaim(String key, String requestHash, Class<T> responseType) {
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findById(key);
        if (existing.isPresent()) {
            return Optional.of(handleExisting(existing.get(), requestHash, responseType));
        }

        try {
            claimTransactionTemplate.executeWithoutResult(status -> {
                IdempotencyKey placeholder = new IdempotencyKey();
                placeholder.setKey(key);
                placeholder.setRequestHash(requestHash);
                placeholder.setExpiresAt(Instant.now().plus(TTL));
                idempotencyKeyRepository.save(placeholder);
            });
            return Optional.empty();
        } catch (DataIntegrityViolationException ex) {
            IdempotencyKey record = idempotencyKeyRepository.findById(key).orElseThrow(() -> ex);
            return Optional.of(handleExisting(record, requestHash, responseType));
        }
    }

    @Transactional
    public void complete(String key, Object response, int httpStatus) {
        IdempotencyKey record = idempotencyKeyRepository.findById(key)
                .orElseThrow(() -> new IllegalStateException("Idempotency key record missing after claim: " + key));
        record.setResponseSnapshot(toJson(response));
        record.setHttpStatus(httpStatus);
        idempotencyKeyRepository.save(record);
    }

    private <T> T handleExisting(IdempotencyKey record, String requestHash, Class<T> responseType) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyKeyReusedException(
                    "Idempotency-Key already used with a different request body: " + record.getKey());
        }
        if (record.getResponseSnapshot() == null) {
            throw new OrderProcessingInProgressException(
                    "Order creation already in progress for this Idempotency-Key: " + record.getKey());
        }
        return deserialize(record.getResponseSnapshot(), responseType);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize response snapshot", ex);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize cached response", ex);
        }
    }
}
