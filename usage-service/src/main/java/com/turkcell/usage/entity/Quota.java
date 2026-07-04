package com.turkcell.usage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "quotas")
public class Quota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false, unique = true)
    private UUID subscriptionId; // subscription-service'e ait, cross-service referans (FK degil)

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "minutes_remaining", nullable = false)
    private int minutesRemaining;

    @Column(name = "sms_remaining", nullable = false)
    private int smsRemaining;

    @Column(name = "mb_remaining", nullable = false)
    private int mbRemaining;

    // ER diyagraminda yok, FR-19'daki %80/%100 esik hesabi icin eklendi: sadece "kalan" bilgisiyle
    // "ne kadari kullanildi" orani hesaplanamaz, orijinal tanimli miktar da saklanmali.
    @Column(name = "minutes_included", nullable = false)
    private int minutesIncluded;

    @Column(name = "sms_included", nullable = false)
    private int smsIncluded;

    @Column(name = "mb_included", nullable = false)
    private int mbIncluded;

    public Quota() {
    }

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
