package com.turkcell.usage.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class QuotaCreateRequest {

    @NotNull(message = "Subscription id cannot be null")
    private UUID subscriptionId;

    @NotNull(message = "Period start cannot be null")
    private LocalDate periodStart;

    @NotNull(message = "Period end cannot be null")
    private LocalDate periodEnd;

    @Min(value = 0, message = "Minutes included cannot be negative")
    private int minutesIncluded;

    @Min(value = 0, message = "SMS included cannot be negative")
    private int smsIncluded;

    @Min(value = 0, message = "MB included cannot be negative")
    private int mbIncluded;

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public int getMinutesIncluded() { return minutesIncluded; }
    public void setMinutesIncluded(int minutesIncluded) { this.minutesIncluded = minutesIncluded; }

    public int getSmsIncluded() { return smsIncluded; }
    public void setSmsIncluded(int smsIncluded) { this.smsIncluded = smsIncluded; }

    public int getMbIncluded() { return mbIncluded; }
    public void setMbIncluded(int mbIncluded) { this.mbIncluded = mbIncluded; }
}
