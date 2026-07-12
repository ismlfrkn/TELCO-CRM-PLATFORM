package com.turkcell.billing.client;

import java.math.BigDecimal;

public class TariffClientDto {

    private String code;
    private String name;
    private BigDecimal monthlyFee;
    private String currency;
    private BigDecimal overageRatePerMinute;
    private BigDecimal overageRateSms;
    private BigDecimal overageRatePerMb;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(BigDecimal monthlyFee) { this.monthlyFee = monthlyFee; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getOverageRatePerMinute() { return overageRatePerMinute; }
    public void setOverageRatePerMinute(BigDecimal overageRatePerMinute) { this.overageRatePerMinute = overageRatePerMinute; }

    public BigDecimal getOverageRateSms() { return overageRateSms; }
    public void setOverageRateSms(BigDecimal overageRateSms) { this.overageRateSms = overageRateSms; }

    public BigDecimal getOverageRatePerMb() { return overageRatePerMb; }
    public void setOverageRatePerMb(BigDecimal overageRatePerMb) { this.overageRatePerMb = overageRatePerMb; }
}
