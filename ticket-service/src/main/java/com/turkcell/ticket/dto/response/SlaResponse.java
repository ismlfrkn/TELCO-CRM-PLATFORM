package com.turkcell.ticket.dto.response;

import java.util.UUID;

public class SlaResponse {

    private UUID id;
    private String category;
    private String priority;
    private int responseTime;
    private int resolutionTime;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getResponseTime() { return responseTime; }
    public void setResponseTime(int responseTime) { this.responseTime = responseTime; }

    public int getResolutionTime() { return resolutionTime; }
    public void setResolutionTime(int resolutionTime) { this.resolutionTime = resolutionTime; }
}
