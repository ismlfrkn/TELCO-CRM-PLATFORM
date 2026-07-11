package com.turkcell.notification.dto.request;

import jakarta.validation.constraints.NotNull;

public class NotificationPreferenceUpdateRequest {

    @NotNull(message = "optedIn cannot be null")
    private Boolean optedIn;

    public Boolean getOptedIn() { return optedIn; }
    public void setOptedIn(Boolean optedIn) { this.optedIn = optedIn; }
}
