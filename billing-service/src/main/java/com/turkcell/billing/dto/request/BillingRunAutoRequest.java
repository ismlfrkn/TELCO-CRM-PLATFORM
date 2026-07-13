package com.turkcell.billing.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * FR-21: manuel tetiklenen ama artik "hangi aboneye ne kadar" bilgisini disaridan istemeyen
 * bill-run modu - sadece donem/vade tarihleri verilir, geri kalani (aktif abone listesi,
 * tarife/aylik ucret) billing-service'in kendisi subscription-service + product-catalog-service'ten
 * senkron olarak turetir (bkz. BillingRunService.runAutomatic).
 */
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
