package com.turkcell.order.client;

import java.util.UUID;

/**
 * customer-service'in CustomerResponse'unun order-service'in ihtiyaç duydugu alt kumesi
 * (consumer-driven contract - tum alanlari degil, sadece kullanilanlari tasir).
 */
public class CustomerClientDto {

    private UUID id;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
