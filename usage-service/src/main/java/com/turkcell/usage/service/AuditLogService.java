package com.turkcell.usage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.usage.entity.AuditLog;
import com.turkcell.usage.repository.AuditLogRepository;
import com.turkcell.usage.security.SecurityUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(String action, String entityType, UUID entityId, Object oldValue, Object newValue) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(SecurityUtils.getCurrentUserId().orElse(null));
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(toJson(oldValue));
        auditLog.setNewValue(toJson(newValue));
        auditLog.setCorrelationId(MDC.get("correlationId"));
        auditLogRepository.save(auditLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
