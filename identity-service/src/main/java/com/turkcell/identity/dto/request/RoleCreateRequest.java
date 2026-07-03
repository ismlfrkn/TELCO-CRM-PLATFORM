package com.turkcell.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RoleCreateRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
