package com.turkcell.order.controller;

import com.turkcell.order.dto.request.OrderCreateRequest;
import com.turkcell.order.dto.response.OrderResponse;
import com.turkcell.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                 @Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(idempotencyKey, request);
    }

    @GetMapping
    public Page<OrderResponse> getByCustomer(@RequestParam UUID customerId, Pageable pageable) {
        return orderService.getOrdersByCustomer(customerId, pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable UUID id) {
        return orderService.getOrderResponseById(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        return orderService.cancelOrder(id);
    }
}
