package com.turkcell.subscription.controller;

import com.turkcell.subscription.dto.request.SubscriptionCreateRequest;
import com.turkcell.subscription.dto.response.SubscriptionResponse;
import com.turkcell.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(@Valid @RequestBody SubscriptionCreateRequest request) {
        return subscriptionService.createSubscription(request);
    }

    @GetMapping("/{id}")
    public SubscriptionResponse getById(@PathVariable UUID id) {
        return subscriptionService.getSubscriptionResponseById(id);
    }

    @GetMapping
    public Page<SubscriptionResponse> getByCustomer(@RequestParam UUID customerId, Pageable pageable) {
        return subscriptionService.getSubscriptionsByCustomer(customerId, pageable);
    }

    @GetMapping("/active")
    public Page<SubscriptionResponse> getActive(Pageable pageable) {
        return subscriptionService.getActiveSubscriptions(pageable);
    }

    @PostMapping("/{id}/suspend")
    public SubscriptionResponse suspend(@PathVariable UUID id) {
        return subscriptionService.suspend(id);
    }

    @PostMapping("/{id}/reactivate")
    public SubscriptionResponse reactivate(@PathVariable UUID id) {
        return subscriptionService.reactivate(id);
    }

    @PostMapping("/{id}/terminate")
    public SubscriptionResponse terminate(@PathVariable UUID id) {
        return subscriptionService.terminate(id);
    }
}
