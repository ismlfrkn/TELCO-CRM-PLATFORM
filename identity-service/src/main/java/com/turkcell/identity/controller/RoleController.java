package com.turkcell.identity.controller;

import com.turkcell.identity.dto.request.RoleCreateRequest;
import com.turkcell.identity.dto.request.RolePatchRequest;
import com.turkcell.identity.dto.request.RoleUpdateRequest;
import com.turkcell.identity.dto.response.RoleResponse;
import com.turkcell.identity.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public Page<RoleResponse> getAll(Pageable pageable) {
        return roleService.getAllRoles(pageable);
    }

    @GetMapping("/{id}")
    public RoleResponse getById(@PathVariable UUID id) {
        return roleService.getRoleResponseById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@Valid @RequestBody RoleCreateRequest request) {
        return roleService.createRole(request);
    }

    @PutMapping("/{id}")
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody RoleUpdateRequest request) {
        return roleService.updateRole(id, request);
    }

    @PatchMapping("/{id}")
    public RoleResponse patch(@PathVariable UUID id, @RequestBody RolePatchRequest request) {
        return roleService.patchRole(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        roleService.deleteRole(id);
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.OK)
    public void assignPermission(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
        roleService.assignPermission(roleId, permissionId);
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
        roleService.revokePermission(roleId, permissionId);
    }
}
