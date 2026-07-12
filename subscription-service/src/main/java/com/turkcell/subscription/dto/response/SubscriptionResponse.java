package com.turkcell.subscription.dto.response;

import java.time.Instant;
import java.util.UUID;

public class SubscriptionResponse {

    private UUID id;
    private UUID customerId;
    private String msisdn;
    private String tariffCode;
    private Integer tariffVersion;
    private String status;
    private Instant activatedAt;
    private Instant terminatedAt;
    private Instant createdAt;
    private UUID orderId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }

    public Integer getTariffVersion() { return tariffVersion; }
    public void setTariffVersion(Integer tariffVersion) { this.tariffVersion = tariffVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }

    public Instant getTerminatedAt() { return terminatedAt; }
    public void setTerminatedAt(Instant terminatedAt) { this.terminatedAt = terminatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
}
