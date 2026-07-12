package com.turkcell.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Nullable: siparis anindaki (aktivasyon ucreti gibi) odemelerin henuz bir faturasi olmaz,
    // fatura aylik bill-run ile sonradan kesilir (order-service saga entegrasyonu icin gevsetildi).
    @Column(name = "invoice_id")
    private UUID invoiceId; // billing-service'e ait, cross-service referans (FK degil)

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String method; // CARD, BANK_TRANSFER, WALLET

    @Column(name = "wallet_id")
    private UUID walletId; // sadece method=WALLET oldugunda dolu, ayni servis icinde referans

    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    // Asagidaki 3 alan Payment'in kendi domain'i degil - OrderCreated event'inden tasinan saga
    // korelasyon verisi (subscription-service'in senkron geri sorgu yapmadan abonelik acabilmesi icin).
    @Column(name = "order_id")
    private UUID orderId; // order-service'e ait, cross-service referans (FK degil)

    @Column(name = "customer_id")
    private UUID customerId; // customer-service'e ait, cross-service referans (FK degil)

    @Column(name = "tariff_code")
    private String tariffCode; // product-catalog-service'e ait, cross-service referans (FK degil)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Payment() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (currency == null) {
            currency = "TRY";
        }
    }

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

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getTariffCode() { return tariffCode; }
    public void setTariffCode(String tariffCode) { this.tariffCode = tariffCode; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
