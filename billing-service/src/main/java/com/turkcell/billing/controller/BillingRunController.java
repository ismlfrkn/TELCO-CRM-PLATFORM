package com.turkcell.billing.controller;

import com.turkcell.billing.dto.request.BillingRunRequest;
import com.turkcell.billing.dto.response.BillingRunResponse;
import com.turkcell.billing.service.BillingRunService;
import jakarta.validation.Valid;
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

    @PostMapping
    public BillingRunResponse run(@Valid @RequestBody BillingRunRequest request) {
        return billingRunService.run(request);
    }
}
