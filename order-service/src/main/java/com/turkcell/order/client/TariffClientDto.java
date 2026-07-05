package com.turkcell.order.client;

import java.math.BigDecimal;

public class TariffClientDto {

    private String code;
    private BigDecimal monthlyFee;
    private String currency;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(BigDecimal monthlyFee) { this.monthlyFee = monthlyFee; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
