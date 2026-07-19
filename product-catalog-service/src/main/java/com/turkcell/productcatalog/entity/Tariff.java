package com.turkcell.productcatalog.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tariffs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "monthly_fee", nullable = false)
    private BigDecimal monthlyFee;

    @Column(name = "minutes_included", nullable = false)
    private int minutesIncluded;

    @Column(name = "sms_included", nullable = false)
    private int smsIncluded;

    @Column(name = "data_mb_included", nullable = false)
    private int dataMbIncluded;

    @Column(name = "overage_rate_per_minute", nullable = false)
    private BigDecimal overageRatePerMinute = BigDecimal.ZERO;

    @Column(name = "overage_rate_per_sms", nullable = false)
    private BigDecimal overageRateSms = BigDecimal.ZERO;

    @Column(name = "overage_rate_per_mb", nullable = false)
    private BigDecimal overageRatePerMb = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToMany
    @JoinTable(
            name = "tariff_addons",
            joinColumns = @JoinColumn(name = "tariff_id"),
            inverseJoinColumns = @JoinColumn(name = "addon_id")
    )
    private Set<Addon> addons = new HashSet<>();

    public Tariff() {
    }

    public Tariff(UUID id, String code, String name, String type, BigDecimal monthlyFee, int minutesIncluded, int smsIncluded, int dataMbIncluded, String status, LocalDate effectiveFrom, LocalDate effectiveTo, String currency, Instant createdAt, Set<Addon> addons) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.monthlyFee = monthlyFee;
        this.minutesIncluded = minutesIncluded;
        this.smsIncluded = smsIncluded;
        this.dataMbIncluded = dataMbIncluded;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.currency = currency;
        this.createdAt = createdAt;
        this.addons = addons != null ? addons : new HashSet<>();
    }

    @PrePersist
    public void prePersist() {
        if (effectiveFrom == null) {
            effectiveFrom = LocalDate.now();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (currency == null) {
            currency = "TRY";
        }
        if (version == 0) {
            version = 1;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(BigDecimal monthlyFee) { this.monthlyFee = monthlyFee; }

    public int getMinutesIncluded() { return minutesIncluded; }
    public void setMinutesIncluded(int minutesIncluded) { this.minutesIncluded = minutesIncluded; }

    public int getSmsIncluded() { return smsIncluded; }
    public void setSmsIncluded(int smsIncluded) { this.smsIncluded = smsIncluded; }

    public int getDataMbIncluded() { return dataMbIncluded; }
    public void setDataMbIncluded(int dataMbIncluded) { this.dataMbIncluded = dataMbIncluded; }

    public BigDecimal getOverageRatePerMinute() { return overageRatePerMinute; }
    public void setOverageRatePerMinute(BigDecimal overageRatePerMinute) { this.overageRatePerMinute = overageRatePerMinute; }

    public BigDecimal getOverageRateSms() { return overageRateSms; }
    public void setOverageRateSms(BigDecimal overageRateSms) { this.overageRateSms = overageRateSms; }

    public BigDecimal getOverageRatePerMb() { return overageRatePerMb; }
    public void setOverageRatePerMb(BigDecimal overageRatePerMb) { this.overageRatePerMb = overageRatePerMb; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Set<Addon> getAddons() { return addons; }
    public void setAddons(Set<Addon> addons) { this.addons = addons; }
}
