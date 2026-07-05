package com.turkcell.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentResponse {

    private UUID id;
    private UUID invoiceId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private UUID walletId;
    private String externalRef;
    private Instant paidAt;
    private String status;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
