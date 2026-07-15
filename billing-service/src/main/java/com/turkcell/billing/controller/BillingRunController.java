package com.turkcell.billing.controller;

import com.turkcell.billing.dto.request.BillingRunAutoRequest;
import com.turkcell.billing.dto.request.BillingRunRequest;
import com.turkcell.billing.dto.response.BillingRunResponse;
import com.turkcell.billing.service.BillingRunService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/runs")
public class BillingRunController {

    private final BillingRunService billingRunService;

    public BillingRunController(BillingRunService billingRunService) {
        this.billingRunService = billingRunService;
    }

    /**
     * FR-21 / spec Bolum 14.2'deki tam kabul kriteri endpoint'i: "Bill-run job manuel tetiklenir"
     * -> abone/tarife bilgisi cagirandan elle alinmaz, subscription-service + product-catalog-service'ten
     * kendisi turetilir (bkz. BillingRunService.runAutomatic). Once burasi elle hazirlanmis
     * InvoiceCreateRequest payload'i bekliyordu (asagidaki /manual'daki davranis) - kabul
     * senaryosunun literal olarak isaret ettigi path bu oldugu icin gercek hesaplama buraya tasindi.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OPERATOR')")
    public BillingRunResponse run(@Valid @RequestBody BillingRunAutoRequest request) {
        return billingRunService.runAutomatic(request);
    }

    /**
     * Elle hazirlanmis fatura kalemleriyle (ör. duzeltme/istisna faturasi) tek seferlik bill-run.
     * FR-21'in otomatik toplu akisinin disinda, admin'in bilinçli olarak override ettigi durum icindir.
     */
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_OPERATOR')")
    public BillingRunResponse runManual(@Valid @RequestBody BillingRunRequest request) {
        return billingRunService.run(request);
    }
}
