package com.turkcell.usage.controller;

import com.turkcell.usage.dto.request.QuotaCreateRequest;
import com.turkcell.usage.dto.response.QuotaResponse;
import com.turkcell.usage.service.QuotaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quotas")
public class QuotaController {

    private final QuotaService quotaService;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuotaResponse create(@Valid @RequestBody QuotaCreateRequest request) {
        return quotaService.createQuota(request);
    }
}
