package com.turkcell.identity.service;

import com.turkcell.identity.dto.request.PermissionCreateRequest;
import com.turkcell.identity.dto.request.PermissionPatchRequest;
import com.turkcell.identity.dto.request.PermissionUpdateRequest;
import com.turkcell.identity.dto.response.PermissionResponse;
import com.turkcell.identity.entity.Permission;
import com.turkcell.identity.exception.DuplicatePermissionCodeException;
import com.turkcell.identity.exception.PermissionNotFoundException;
import com.turkcell.identity.mapper.PermissionMapper;
import com.turkcell.identity.repository.PermissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final AuditLogService auditLogService;

    public PermissionService(PermissionRepository permissionRepository, PermissionMapper permissionMapper, AuditLogService auditLogService) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
        this.auditLogService = auditLogService;
    }

    public Page<PermissionResponse> getAllPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable).map(permissionMapper::toResponse);
    }

    public PermissionResponse getPermissionResponseById(UUID id) {
        return permissionMapper.toResponse(getPermissionById(id));
    }

    public Permission getPermissionById(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new PermissionNotFoundException("Permission not found with id: " + id));
    }

    @Transactional
    public PermissionResponse createPermission(PermissionCreateRequest request) {
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new DuplicatePermissionCodeException("Permission code already in use: " + request.getCode());
        }

        Permission permission = new Permission();
        permission.setCode(request.getCode());
        permission.setDescription(request.getDescription());

        Permission saved = permissionRepository.save(permission);
        auditLogService.record("CREATE", "Permission", saved.getId(), null, saved.getCode());
        return permissionMapper.toResponse(saved);
    }

    @Transactional
    public PermissionResponse updatePermission(UUID id, PermissionUpdateRequest request) {
        Permission permission = getPermissionById(id);
        String before = permission.getCode();

        if (!permission.getCode().equals(request.getCode()) && permissionRepository.existsByCode(request.getCode())) {
            throw new DuplicatePermissionCodeException("Permission code already in use: " + request.getCode());
        }

        permission.setCode(request.getCode());
        permission.setDescription(request.getDescription());

        Permission saved = permissionRepository.save(permission);
        auditLogService.record("UPDATE", "Permission", saved.getId(), before, saved.getCode());
        return permissionMapper.toResponse(saved);
    }

    @Transactional
    public PermissionResponse patchPermission(UUID id, PermissionPatchRequest request) {
        Permission permission = getPermissionById(id);
        String before = permission.getCode();

        if (request.getCode() != null && !request.getCode().equals(permission.getCode())) {
            if (permissionRepository.existsByCode(request.getCode())) {
                throw new DuplicatePermissionCodeException("Permission code already in use: " + request.getCode());
            }
            permission.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }

        Permission saved = permissionRepository.save(permission);
        auditLogService.record("UPDATE", "Permission", saved.getId(), before, saved.getCode());
        return permissionMapper.toResponse(saved);
    }

    @Transactional
    public void deletePermission(UUID id) {
        Permission permission = getPermissionById(id);
        permissionRepository.delete(permission);
        auditLogService.record("DELETE", "Permission", id, permission.getCode(), null);
    }
}
