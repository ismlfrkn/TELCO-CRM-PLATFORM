package com.turkcell.identity.dto.response;

import java.util.List;
import java.util.UUID;

public class RoleResponse {

    private UUID id;
    private String name;
    private String description;
    private List<PermissionResponse> permissions;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<PermissionResponse> getPermissions() { return permissions; }
    public void setPermissions(List<PermissionResponse> permissions) { this.permissions = permissions; }
}
