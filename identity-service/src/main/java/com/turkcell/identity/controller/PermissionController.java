package com.turkcell.identity.controller;

import com.turkcell.identity.dto.request.PermissionCreateRequest;
import com.turkcell.identity.dto.request.PermissionPatchRequest;
import com.turkcell.identity.dto.request.PermissionUpdateRequest;
import com.turkcell.identity.dto.response.PermissionResponse;
import com.turkcell.identity.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/permissions")
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public Page<PermissionResponse> getAll(Pageable pageable) {
        return permissionService.getAllPermissions(pageable);
    }

    @GetMapping("/{id}")
    public PermissionResponse getById(@PathVariable UUID id) {
        return permissionService.getPermissionResponseById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse create(@Valid @RequestBody PermissionCreateRequest request) {
        return permissionService.createPermission(request);
    }

    @PutMapping("/{id}")
    public PermissionResponse update(@PathVariable UUID id, @Valid @RequestBody PermissionUpdateRequest request) {
        return permissionService.updatePermission(id, request);
    }

    @PatchMapping("/{id}")
    public PermissionResponse patch(@PathVariable UUID id, @RequestBody PermissionPatchRequest request) {
        return permissionService.patchPermission(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        permissionService.deletePermission(id);
    }
}
