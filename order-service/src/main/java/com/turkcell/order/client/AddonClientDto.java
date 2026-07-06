package com.turkcell.order.client;

import java.math.BigDecimal;

public class AddonClientDto {

    private String code;
    private BigDecimal price;
    private String currency;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
