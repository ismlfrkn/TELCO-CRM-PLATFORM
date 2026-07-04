package com.turkcell.usage.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public class QuotaResponse {

    private UUID id;
    private UUID subscriptionId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int minutesRemaining;
    private int smsRemaining;
    private int mbRemaining;
    private int minutesIncluded;
    private int smsIncluded;
    private int mbIncluded;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public int getMinutesRemaining() { return minutesRemaining; }
    public void setMinutesRemaining(int minutesRemaining) { this.minutesRemaining = minutesRemaining; }

    public int getSmsRemaining() { return smsRemaining; }
    public void setSmsRemaining(int smsRemaining) { this.smsRemaining = smsRemaining; }

    public int getMbRemaining() { return mbRemaining; }
    public void setMbRemaining(int mbRemaining) { this.mbRemaining = mbRemaining; }

    public int getMinutesIncluded() { return minutesIncluded; }
    public void setMinutesIncluded(int minutesIncluded) { this.minutesIncluded = minutesIncluded; }

    public int getSmsIncluded() { return smsIncluded; }
    public void setSmsIncluded(int smsIncluded) { this.smsIncluded = smsIncluded; }

    public int getMbIncluded() { return mbIncluded; }
    public void setMbIncluded(int mbIncluded) { this.mbIncluded = mbIncluded; }
}
