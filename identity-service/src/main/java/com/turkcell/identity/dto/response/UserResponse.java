package com.turkcell.identity.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String phoneNumber;
    private String status;
    private UUID customerId;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<RoleResponse> roles;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<RoleResponse> getRoles() { return roles; }
    public void setRoles(List<RoleResponse> roles) { this.roles = roles; }
}
