package com.turkcell.billing.controller;

import com.turkcell.billing.dto.request.InvoiceCreateRequest;
import com.turkcell.billing.dto.response.InvoiceResponse;
import com.turkcell.billing.service.InvoicePdfService;
import com.turkcell.billing.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    public InvoiceController(InvoiceService invoiceService, InvoicePdfService invoicePdfService) {
        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody InvoiceCreateRequest request) {
        return invoiceService.createInvoice(request);
    }

    @GetMapping
    public Page<InvoiceResponse> getByCustomer(@RequestParam UUID customerId, Pageable pageable) {
        return invoiceService.getInvoicesByCustomer(customerId, pageable);
    }

    @GetMapping("/{id}")
    public InvoiceResponse getById(@PathVariable UUID id) {
        return invoiceService.getInvoiceResponseById(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable UUID id) {
        InvoiceResponse invoice = invoiceService.getInvoiceResponseById(id);
        byte[] pdf = invoicePdfService.generate(invoice);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OPERATOR')")
    public InvoiceResponse markPaid(@PathVariable UUID id) {
        return invoiceService.markPaid(id);
    }

    @PostMapping("/{id}/mark-overdue")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OPERATOR')")
    public InvoiceResponse markOverdue(@PathVariable UUID id) {
        return invoiceService.markOverdue(id);
    }
}
