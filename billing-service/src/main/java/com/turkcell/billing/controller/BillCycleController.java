package com.turkcell.billing.controller;

import com.turkcell.billing.dto.request.BillCycleCreateRequest;
import com.turkcell.billing.dto.response.BillCycleResponse;
import com.turkcell.billing.service.BillCycleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bill-cycles")
public class BillCycleController {

    private final BillCycleService billCycleService;

    public BillCycleController(BillCycleService billCycleService) {
        this.billCycleService = billCycleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillCycleResponse create(@Valid @RequestBody BillCycleCreateRequest request) {
        return billCycleService.createBillCycle(request);
    }

    @GetMapping("/{customerId}")
    public BillCycleResponse getByCustomerId(@PathVariable UUID customerId) {
        return billCycleService.getBillCycleResponse(customerId);
    }
}
