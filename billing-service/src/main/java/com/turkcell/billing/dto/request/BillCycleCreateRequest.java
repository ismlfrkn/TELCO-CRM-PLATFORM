package com.turkcell.billing.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class BillCycleCreateRequest {

    @NotNull(message = "Customer id cannot be null")
    private UUID customerId;

    @Min(value = 1, message = "Day of month must be between 1 and 28")
    @Max(value = 28, message = "Day of month must be between 1 and 28")
    private int dayOfMonth;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }
}
