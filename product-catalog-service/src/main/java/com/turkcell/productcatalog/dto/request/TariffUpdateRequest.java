package com.turkcell.productcatalog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TariffUpdateRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Type cannot be blank")
    private String type;

    @NotNull(message = "Monthly fee cannot be null")
    @Min(value = 0, message = "Monthly fee cannot be negative")
    private BigDecimal monthlyFee;

    @Min(value = 0, message = "Minutes cannot be negative")
    private int minutesIncluded;

    @Min(value = 0, message = "SMS cannot be negative")
    private int smsIncluded;

    @Min(value = 0, message = "Data MB cannot be negative")
    private int dataMbIncluded;

    @Min(value = 0, message = "Overage rate per minute cannot be negative")
    private BigDecimal overageRatePerMinute = BigDecimal.ZERO;

    @Min(value = 0, message = "Overage rate per sms cannot be negative")
    private BigDecimal overageRateSms = BigDecimal.ZERO;

    @Min(value = 0, message = "Overage rate per mb cannot be negative")
    private BigDecimal overageRatePerMb = BigDecimal.ZERO;

    @NotBlank(message = "Status cannot be blank")
    private String status;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency;

    @NotNull(message = "Effective from date cannot be null")
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

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

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
}
