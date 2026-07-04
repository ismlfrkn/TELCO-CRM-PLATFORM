package com.turkcell.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId; // customer-service'e ait, cross-service referans (FK degil)

    @Column(nullable = false)
    private String msisdn; // msisdn_pool'a ait, ayni servis icinde referans

    @Column(name = "tariff_code", nullable = false)
    private String tariffCode; // product-catalog-service'e ait, cross-service referans (FK degil)

    @Column(nullable = false)
    private String status; // ACTIVE, SUSPENDED, TERMINATED

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Subscription() {
    }

    public Subscription(UUID id, UUID customerId, String msisdn, String tariffCode, String status, Instant activatedAt, Instant terminatedAt, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.msisdn = msisdn;
        this.tariffCode = tariffCode;
        this.status = status;
        this.activatedAt = activatedAt;
        this.terminatedAt = terminatedAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }

    public Instant getTerminatedAt() { return terminatedAt; }
    public void setTerminatedAt(Instant terminatedAt) { this.terminatedAt = terminatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
