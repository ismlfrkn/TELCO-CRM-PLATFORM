package com.turkcell.productcatalog.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TariffPatchRequest {

    private String name;
    private String type;
    private BigDecimal monthlyFee;
    private Integer minutesIncluded;
    private Integer smsIncluded;
    private Integer dataMbIncluded;
    private BigDecimal overageRatePerMinute;
    private BigDecimal overageRateSms;
    private BigDecimal overageRatePerMb;
    private String status;
    private String currency;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(BigDecimal monthlyFee) { this.monthlyFee = monthlyFee; }

    public Integer getMinutesIncluded() { return minutesIncluded; }
    public void setMinutesIncluded(Integer minutesIncluded) { this.minutesIncluded = minutesIncluded; }

    public Integer getSmsIncluded() { return smsIncluded; }
    public void setSmsIncluded(Integer smsIncluded) { this.smsIncluded = smsIncluded; }

    public Integer getDataMbIncluded() { return dataMbIncluded; }
    public void setDataMbIncluded(Integer dataMbIncluded) { this.dataMbIncluded = dataMbIncluded; }

    public BigDecimal getOverageRatePerMinute() { return overageRatePerMinute; }
    public void setOverageRatePerMinute(BigDecimal overageRatePerMinute) { this.overageRatePerMinute = overageRatePerMinute; }

    public BigDecimal getOverageRateSms() { return overageRateSms; }
    public void setOverageRateSms(BigDecimal overageRateSms) { this.overageRateSms = overageRateSms; }

    public BigDecimal getOverageRatePerMb() { return overageRatePerMb; }
    public void setOverageRatePerMb(BigDecimal overageRatePerMb) { this.overageRatePerMb = overageRatePerMb; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
}
