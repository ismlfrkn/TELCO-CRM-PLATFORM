package com.turkcell.ticket.dto.response;

import java.time.Instant;
import java.util.UUID;

public class TicketResponse {

    private UUID id;
    private UUID customerId;
    private String category;
    private String priority;
    private String status;
    private Instant slaDueAt;
    private Instant createdAt;
    private UUID slaId;
    private String assignedTeam;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSlaDueAt() { return slaDueAt; }
    public void setSlaDueAt(Instant slaDueAt) { this.slaDueAt = slaDueAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public UUID getSlaId() { return slaId; }
    public void setSlaId(UUID slaId) { this.slaId = slaId; }

    public String getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(String assignedTeam) { this.assignedTeam = assignedTeam; }
}
