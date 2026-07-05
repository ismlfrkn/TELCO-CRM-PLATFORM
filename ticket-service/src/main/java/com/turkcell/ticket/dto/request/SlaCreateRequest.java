package com.turkcell.ticket.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SlaCreateRequest {

    @NotBlank(message = "Category cannot be blank")
    private String category;

    @NotBlank(message = "Priority cannot be blank")
    private String priority;

    @Min(value = 1, message = "Response time must be at least 1 minute")
    private int responseTime;

    @Min(value = 1, message = "Resolution time must be at least 1 minute")
    private int resolutionTime;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getResponseTime() { return responseTime; }
    public void setResponseTime(int responseTime) { this.responseTime = responseTime; }

    public int getResolutionTime() { return resolutionTime; }
    public void setResolutionTime(int resolutionTime) { this.resolutionTime = resolutionTime; }
}
