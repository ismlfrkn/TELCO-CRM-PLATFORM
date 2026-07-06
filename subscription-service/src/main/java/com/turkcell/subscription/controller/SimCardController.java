package com.turkcell.subscription.controller;

import com.turkcell.subscription.dto.request.SimCardAssignRequest;
import com.turkcell.subscription.dto.request.SimCardRegisterRequest;
import com.turkcell.subscription.dto.response.SimCardResponse;
import com.turkcell.subscription.service.SimCardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sim-cards")
public class SimCardController {

    private final SimCardService simCardService;

    public SimCardController(SimCardService simCardService) {
        this.simCardService = simCardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimCardResponse register(@Valid @RequestBody SimCardRegisterRequest request) {
        return simCardService.registerSimCard(request);
    }

    @GetMapping("/{iccid}")
    public SimCardResponse getByIccid(@PathVariable String iccid) {
        return simCardService.getSimCardResponse(iccid);
    }

    @PostMapping("/{iccid}/assign")
    public SimCardResponse assign(@PathVariable String iccid, @Valid @RequestBody SimCardAssignRequest request) {
        return simCardService.assignMsisdn(iccid, request);
    }
}
