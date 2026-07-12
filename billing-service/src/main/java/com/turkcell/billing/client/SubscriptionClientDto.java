package com.turkcell.billing.client;

import java.util.UUID;

public class SubscriptionClientDto {

    private UUID id;
    private UUID customerId;
    private String tariffCode;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
