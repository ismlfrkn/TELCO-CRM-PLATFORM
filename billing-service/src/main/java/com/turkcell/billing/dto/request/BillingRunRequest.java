package com.turkcell.billing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BillingRunRequest {

    @NotEmpty(message = "At least one invoice is required")
    @Valid
    private List<InvoiceCreateRequest> invoices;

    public List<InvoiceCreateRequest> getInvoices() { return invoices; }
    public void setInvoices(List<InvoiceCreateRequest> invoices) { this.invoices = invoices; }
}
