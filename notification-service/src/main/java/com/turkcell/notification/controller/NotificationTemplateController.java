package com.turkcell.notification.controller;

import com.turkcell.notification.dto.request.NotificationTemplateCreateRequest;
import com.turkcell.notification.dto.response.NotificationTemplateResponse;
import com.turkcell.notification.service.NotificationTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-templates")
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    public NotificationTemplateController(NotificationTemplateService notificationTemplateService) {
        this.notificationTemplateService = notificationTemplateService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationTemplateResponse create(@Valid @RequestBody NotificationTemplateCreateRequest request) {
        return notificationTemplateService.createTemplate(request);
    }
}
