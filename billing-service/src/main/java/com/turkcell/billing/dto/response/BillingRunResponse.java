package com.turkcell.billing.dto.response;

import java.util.List;

public class BillingRunResponse {

    private int requested;
    private int processed;
    private List<InvoiceResponse> invoices;

    public int getRequested() { return requested; }
    public void setRequested(int requested) { this.requested = requested; }

    public int getProcessed() { return processed; }
    public void setProcessed(int processed) { this.processed = processed; }

    public List<InvoiceResponse> getInvoices() { return invoices; }
    public void setInvoices(List<InvoiceResponse> invoices) { this.invoices = invoices; }
}
