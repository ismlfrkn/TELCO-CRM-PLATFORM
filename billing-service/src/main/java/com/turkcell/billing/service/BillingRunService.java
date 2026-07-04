package com.turkcell.billing.service;

import com.turkcell.billing.dto.request.BillingRunRequest;
import com.turkcell.billing.dto.request.InvoiceCreateRequest;
import com.turkcell.billing.dto.response.BillingRunResponse;
import com.turkcell.billing.dto.response.InvoiceResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-21: "Aylik bill-run job'u ... fatura keser" - dokumanin kendi kabul kriterinde (bolum 14.2)
 * bu job'un MANUEL tetiklendigi acikca belirtiliyor, bu yuzden zamanlanmis (scheduled) bir gorev
 * kurulmadi; bu endpoint tam olarak o manuel tetiklemeyi temsil eder. Her satir kendi icinde
 * idempotent oldugu (InvoiceService.createInvoice) icin bill-run yanlislikla iki kez calistirilirsa
 * da cift fatura olusmaz.
 */
@Service
public class BillingRunService {

    private final InvoiceService invoiceService;

    public BillingRunService(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public BillingRunResponse run(BillingRunRequest request) {
        List<InvoiceResponse> invoices = new ArrayList<>();
        for (InvoiceCreateRequest invoiceRequest : request.getInvoices()) {
            invoices.add(invoiceService.createInvoice(invoiceRequest));
        }

        BillingRunResponse response = new BillingRunResponse();
        response.setRequested(request.getInvoices().size());
        response.setProcessed(invoices.size());
        response.setInvoices(invoices);
        return response;
    }
}
