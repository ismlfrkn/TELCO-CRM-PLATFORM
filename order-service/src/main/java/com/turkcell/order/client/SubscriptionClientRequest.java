package com.turkcell.order.client;

import java.util.UUID;

public class SubscriptionClientRequest {

    private UUID customerId;
    private String tariffCode;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }
}
