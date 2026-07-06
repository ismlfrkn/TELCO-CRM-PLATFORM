package com.turkcell.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class OrderCreateRequest {

    @NotNull(message = "Customer id cannot be null")
    private UUID customerId;

    @NotEmpty(message = "At least one order item is required")
    @Valid
    private List<OrderItemRequest> items;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
