package com.turkcell.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletCreateRequest {

    @NotNull(message = "Customer id cannot be null")
    private UUID customerId;

    @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
    private BigDecimal initialBalance;

    private String currency;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
