package com.turkcell.order.client;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentClientRequest {

    private UUID invoiceId; // siparis anindaki odemede bos birakilir
    private BigDecimal amount;
    private String currency;
    private String method;
    private UUID walletId;
    private boolean simulateFailure;

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }

    public boolean isSimulateFailure() { return simulateFailure; }
    public void setSimulateFailure(boolean simulateFailure) { this.simulateFailure = simulateFailure; }
}
