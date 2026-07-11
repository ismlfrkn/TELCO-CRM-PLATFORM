package com.turkcell.notification.controller;

import com.turkcell.notification.dto.request.NotificationPreferenceUpdateRequest;
import com.turkcell.notification.dto.response.NotificationPreferenceResponse;
import com.turkcell.notification.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;

    public NotificationPreferenceController(NotificationPreferenceService notificationPreferenceService) {
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @PutMapping("/users/{userId}/channels/{channelCode}")
    public NotificationPreferenceResponse setPreference(@PathVariable UUID userId,
                                                          @PathVariable String channelCode,
                                                          @Valid @RequestBody NotificationPreferenceUpdateRequest request) {
        return notificationPreferenceService.setPreference(userId, channelCode, request.getOptedIn());
    }

    @GetMapping("/users/{userId}")
    public List<NotificationPreferenceResponse> getPreferences(@PathVariable UUID userId) {
        return notificationPreferenceService.getPreferences(userId);
    }
}
