package com.turkcell.billing.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public class BillCycleResponse {

    private UUID id;
    private UUID customerId;
    private int dayOfMonth;
    private LocalDate nextRunDate;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public LocalDate getNextRunDate() { return nextRunDate; }
    public void setNextRunDate(LocalDate nextRunDate) { this.nextRunDate = nextRunDate; }
}
