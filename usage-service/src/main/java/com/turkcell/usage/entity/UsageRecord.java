package com.turkcell.usage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_records")
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false)
    private String type; // VOICE, SMS, DATA

    @Column(nullable = false)
    private BigDecimal quantity; // VOICE: dakika, SMS: adet, DATA: MB

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "cdr_ref")
    private String cdrRef; // CdrEvent.externalCdrId

    public UsageRecord() {
    }

    @PrePersist
    public void prePersist() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public String getCdrRef() { return cdrRef; }
    public void setCdrRef(String cdrRef) { this.cdrRef = cdrRef; }
}
