package com.turkcell.subscription.controller;

import com.turkcell.subscription.dto.request.MsisdnPoolAddRequest;
import com.turkcell.subscription.dto.response.MsisdnPoolResponse;
import com.turkcell.subscription.service.MsisdnPoolService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/msisdn-pool")
public class MsisdnPoolController {

    private final MsisdnPoolService msisdnPoolService;

    public MsisdnPoolController(MsisdnPoolService msisdnPoolService) {
        this.msisdnPoolService = msisdnPoolService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MsisdnPoolResponse add(@Valid @RequestBody MsisdnPoolAddRequest request) {
        return msisdnPoolService.addToPool(request);
    }

    @GetMapping
    public Page<MsisdnPoolResponse> list(@RequestParam(required = false) String status, Pageable pageable) {
        return msisdnPoolService.list(status, pageable);
    }
}
