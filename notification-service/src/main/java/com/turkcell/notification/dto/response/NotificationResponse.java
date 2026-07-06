package com.turkcell.notification.dto.response;

import java.time.Instant;
import java.util.UUID;

public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private String templateCode;
    private UUID channelId;
    private String payloadJson;
    private String status;
    private Instant sentAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public UUID getChannelId() { return channelId; }
    public void setChannelId(UUID channelId) { this.channelId = channelId; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
