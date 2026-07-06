package com.turkcell.usage.controller;

import com.turkcell.usage.dto.response.QuotaResponse;
import com.turkcell.usage.dto.response.UsageRecordResponse;
import com.turkcell.usage.service.QuotaService;
import com.turkcell.usage.service.UsageHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usage/subscriptions")
public class UsageController {

    private final QuotaService quotaService;
    private final UsageHistoryService usageHistoryService;

    public UsageController(QuotaService quotaService, UsageHistoryService usageHistoryService) {
        this.quotaService = quotaService;
        this.usageHistoryService = usageHistoryService;
    }

    @GetMapping("/{subscriptionId}/quota")
    public QuotaResponse getQuota(@PathVariable UUID subscriptionId) {
        return quotaService.getQuotaResponse(subscriptionId);
    }

    @GetMapping("/{subscriptionId}/history")
    public Page<UsageRecordResponse> getHistory(
            @PathVariable UUID subscriptionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);
        return usageHistoryService.getHistory(subscriptionId, effectiveFrom, effectiveTo, pageable);
    }
}
