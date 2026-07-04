package com.turkcell.payment.controller;

import com.turkcell.payment.dto.request.PaymentCreateRequest;
import com.turkcell.payment.dto.response.PaymentResponse;
import com.turkcell.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                   @Valid @RequestBody PaymentCreateRequest request) {
        return paymentService.createPayment(idempotencyKey, request);
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable UUID id) {
        return paymentService.getPaymentResponseById(id);
    }

    @PostMapping("/{id}/retry")
    public PaymentResponse retry(@PathVariable UUID id) {
        return paymentService.retry(id);
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable UUID id) {
        return paymentService.refund(id);
    }
}
