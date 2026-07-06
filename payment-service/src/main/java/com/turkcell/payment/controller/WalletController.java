package com.turkcell.payment.controller;

import com.turkcell.payment.dto.request.WalletCreateRequest;
import com.turkcell.payment.dto.request.WalletTopUpRequest;
import com.turkcell.payment.dto.response.WalletResponse;
import com.turkcell.payment.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse create(@Valid @RequestBody WalletCreateRequest request) {
        return walletService.createWallet(request);
    }

    @GetMapping("/{id}")
    public WalletResponse getById(@PathVariable UUID id) {
        return walletService.getWalletResponse(id);
    }

    @PostMapping("/{id}/top-up")
    public WalletResponse topUp(@PathVariable UUID id, @Valid @RequestBody WalletTopUpRequest request) {
        return walletService.topUp(id, request);
    }
}
