package com.turkcell.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public class NotificationSendRequest {

    @NotNull(message = "User id cannot be null")
    private UUID userId;

    @NotBlank(message = "Template code cannot be blank")
    private String templateCode;

    @NotBlank(message = "Channel code cannot be blank")
    private String channelCode;

    private String locale = "tr-TR";

    // Sablondaki {{placeholder}} alanlarini doldurmak icin kullanilir, ayrica payloadJson olarak saklanir.
    private Map<String, String> payload;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public Map<String, String> getPayload() { return payload; }
    public void setPayload(Map<String, String> payload) { this.payload = payload; }
}
