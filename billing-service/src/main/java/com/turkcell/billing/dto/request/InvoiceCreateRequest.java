package com.turkcell.billing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class InvoiceCreateRequest {

    @NotNull(message = "Customer id cannot be null")
    private UUID customerId;

    @NotNull(message = "Subscription id cannot be null")
    private UUID subscriptionId;

    @NotNull(message = "Period start cannot be null")
    private LocalDate periodStart;

    @NotNull(message = "Period end cannot be null")
    private LocalDate periodEnd;

    @NotNull(message = "Due date cannot be null")
    private LocalDate dueDate;

    @NotEmpty(message = "At least one invoice line is required")
    @Valid
    private List<InvoiceLineRequest> lines;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public List<InvoiceLineRequest> getLines() { return lines; }
    public void setLines(List<InvoiceLineRequest> lines) { this.lines = lines; }
}
