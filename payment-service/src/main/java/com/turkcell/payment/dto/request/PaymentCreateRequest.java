package com.turkcell.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentCreateRequest {

    // Siparis anindaki (aktivasyon ucreti gibi) odemelerde bos birakilir - fatura henuz kesilmemistir.
    private UUID invoiceId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    // Bos birakilirsa Payment.prePersist varsayilan olarak "TRY" atar (Wallet/Order ile ayni pattern).
    private String currency;

    @NotBlank(message = "Method cannot be blank")
    private String method; // CARD, BANK_TRANSFER, WALLET

    // method=WALLET oldugunda zorunlu, diger yontemlerde yok sayilir
    private UUID walletId;

    // Gercek bir PSP entegrasyonu olmadigi icin test amacli: true ise mock PSP odemeyi reddeder.
    private boolean simulateFailure;

    // Sadece order-service'ten tuketilen OrderCreated event'i uzerinden gelen odemelerde dolu -
    // saga korelasyon verisi, REST uzerinden dogrudan cagrilan odemelerde bos kalir.
    private UUID orderId;
    private UUID customerId;
    private String tariffCode;

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

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }
}
