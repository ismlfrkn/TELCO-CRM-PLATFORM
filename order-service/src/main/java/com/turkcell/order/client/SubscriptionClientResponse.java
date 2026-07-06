package com.turkcell.order.client;

import java.util.UUID;

public class SubscriptionClientResponse {

    private UUID id;
    private String msisdn;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
