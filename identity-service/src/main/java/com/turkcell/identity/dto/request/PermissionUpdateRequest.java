package com.turkcell.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public class PermissionUpdateRequest {

    @NotBlank(message = "Code cannot be blank")
    private String code;

    private String description;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
