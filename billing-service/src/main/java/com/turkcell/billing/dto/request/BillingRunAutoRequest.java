package com.turkcell.billing.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class BillingRunAutoRequest {

    @NotNull(message = "Period start cannot be null")
    private LocalDate periodStart;

    @NotNull(message = "Period end cannot be null")
    private LocalDate periodEnd;

    @NotNull(message = "Due date cannot be null")
    private LocalDate dueDate;

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
