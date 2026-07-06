package com.turkcell.usage.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class UsageRecordResponse {

    private UUID id;
    private UUID subscriptionId;
    private String type;
    private BigDecimal quantity;
    private Instant recordedAt;
    private String cdrRef;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public String getCdrRef() { return cdrRef; }
    public void setCdrRef(String cdrRef) { this.cdrRef = cdrRef; }
}
