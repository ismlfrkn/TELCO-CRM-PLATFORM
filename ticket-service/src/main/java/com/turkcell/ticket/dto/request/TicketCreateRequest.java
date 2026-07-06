package com.turkcell.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class TicketCreateRequest {

    @NotNull(message = "Customer id cannot be null")
    private UUID customerId;

    @NotBlank(message = "Category cannot be blank")
    private String category;

    @NotBlank(message = "Priority cannot be blank")
    private String priority;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
