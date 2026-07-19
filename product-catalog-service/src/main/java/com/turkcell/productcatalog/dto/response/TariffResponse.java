package com.turkcell.productcatalog.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TariffResponse {

    private UUID id;
    private String code;
    private int version;
    private String name;
    private String type;
    private BigDecimal monthlyFee;
    private int minutesIncluded;
    private int smsIncluded;
    private int dataMbIncluded;
    private BigDecimal overageRatePerMinute;
    private BigDecimal overageRateSms;
    private BigDecimal overageRatePerMb;
    private String status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String currency;
    private Instant createdAt;
    private List<AddonResponse> addons;

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

    public List<AddonResponse> getAddons() { return addons; }
    public void setAddons(List<AddonResponse> addons) { this.addons = addons; }
}
