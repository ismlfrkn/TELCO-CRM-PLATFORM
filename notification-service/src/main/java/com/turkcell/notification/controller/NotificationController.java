package com.turkcell.notification.controller;

import com.turkcell.notification.dto.request.NotificationSendRequest;
import com.turkcell.notification.dto.response.NotificationResponse;
import com.turkcell.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse send(@Valid @RequestBody NotificationSendRequest request) {
        return notificationService.send(request);
    }

    @GetMapping("/users/{id}/history")
    public Page<NotificationResponse> getHistory(@PathVariable UUID id, Pageable pageable) {
        return notificationService.getHistoryForUser(id, pageable);
    }
}
