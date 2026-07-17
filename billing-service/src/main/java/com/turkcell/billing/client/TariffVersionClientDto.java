package com.turkcell.billing.client;

import java.math.BigDecimal;

public class TariffVersionClientDto {

    private String code;
    private int version;
    private String name;
    private BigDecimal monthlyFee;
    private String currency;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(BigDecimal monthlyFee) { this.monthlyFee = monthlyFee; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
