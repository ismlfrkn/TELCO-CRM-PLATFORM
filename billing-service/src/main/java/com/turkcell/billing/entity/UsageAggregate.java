package com.turkcell.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * FR-20: usage-service'in UsageAggregated event'iyle bildirdigi asim kullanimlarinin billing
 * tarafinda biriktirildigi (agregasyon) tablo. invoiceId NULL oldugu surece "unclaimed" sayilir;
 * InvoiceService bir bill-run'da bu satiri bir faturaya donusturdugunde invoiceId'yi set ederek
 * ayni asimin bir sonraki bill-run'da tekrar faturalanmasini (cift ucretlendirmeyi) engeller.
 */
@Entity
@Table(name = "usage_aggregates")
public class UsageAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "quota_id")
    private UUID quotaId;

    @Column(name = "cdr_type", nullable = false)
    private String cdrType;

    @Column(name = "overage_quantity", nullable = false)
    private BigDecimal overageQuantity;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    // usage-service outbox'undaki event id'si - ayni event en-az-bir-kez teslimatla tekrar
    // gelirse UNIQUE constraint sayesinde ikinci kez islenmez (idempotent consumer).
    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    // NULL: henuz faturalanmamis (unclaimed). Dolu: bu asim hangi faturaya islendi.
    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public UsageAggregate() {
    }

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public UUID getQuotaId() { return quotaId; }
    public void setQuotaId(UUID quotaId) { this.quotaId = quotaId; }

    public String getCdrType() { return cdrType; }
    public void setCdrType(String cdrType) { this.cdrType = cdrType; }

    public BigDecimal getOverageQuantity() { return overageQuantity; }
    public void setOverageQuantity(BigDecimal overageQuantity) { this.overageQuantity = overageQuantity; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public UUID getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(UUID sourceEventId) { this.sourceEventId = sourceEventId; }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
