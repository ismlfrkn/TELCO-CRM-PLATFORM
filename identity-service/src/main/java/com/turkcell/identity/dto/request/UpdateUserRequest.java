package com.turkcell.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class UpdateUserRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Phone number cannot be blank")
    private String phoneNumber;

    @NotBlank(message = "Status cannot be blank")
    private String status;

    private UUID customerId;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
}
