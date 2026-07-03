package com.turkcell.identity.service;

import com.turkcell.identity.dto.request.RoleCreateRequest;
import com.turkcell.identity.dto.request.RolePatchRequest;
import com.turkcell.identity.dto.request.RoleUpdateRequest;
import com.turkcell.identity.dto.response.PermissionResponse;
import com.turkcell.identity.dto.response.RoleResponse;
import com.turkcell.identity.entity.Permission;
import com.turkcell.identity.entity.Role;
import com.turkcell.identity.entity.RolePermission;
import com.turkcell.identity.entity.RolePermissionId;
import com.turkcell.identity.exception.DuplicateRoleNameException;
import com.turkcell.identity.exception.RoleNotFoundException;
import com.turkcell.identity.mapper.PermissionMapper;
import com.turkcell.identity.mapper.RoleMapper;
import com.turkcell.identity.repository.RolePermissionRepository;
import com.turkcell.identity.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionService permissionService;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final AuditLogService auditLogService;

    public RoleService(RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository,
                        PermissionService permissionService, RoleMapper roleMapper, PermissionMapper permissionMapper,
                        AuditLogService auditLogService) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionService = permissionService;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.auditLogService = auditLogService;
    }

    public Page<RoleResponse> getAllRoles(Pageable pageable) {
        return roleRepository.findAll(pageable).map(this::toResponseWithPermissions);
    }

    public RoleResponse getRoleResponseById(UUID id) {
        return toResponseWithPermissions(getRoleById(id));
    }

    public Role getRoleById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + id));
    }

    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with name: " + name));
    }

    public List<PermissionResponse> getPermissionsForRole(UUID roleId) {
        return rolePermissionRepository.findAllByRoleId(roleId).stream()
                .map(RolePermission::getPermission)
                .map(permissionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateRoleNameException("Role name already in use: " + request.getName());
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        auditLogService.record("CREATE", "Role", saved.getId(), null, saved.getName());
        return toResponseWithPermissions(saved);
    }

    @Transactional
    public RoleResponse updateRole(UUID id, RoleUpdateRequest request) {
        Role role = getRoleById(id);
        String before = role.getName();

        if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw new DuplicateRoleNameException("Role name already in use: " + request.getName());
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        auditLogService.record("UPDATE", "Role", saved.getId(), before, saved.getName());
        return toResponseWithPermissions(saved);
    }

    @Transactional
    public RoleResponse patchRole(UUID id, RolePatchRequest request) {
        Role role = getRoleById(id);
        String before = role.getName();

        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByName(request.getName())) {
                throw new DuplicateRoleNameException("Role name already in use: " + request.getName());
            }
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        Role saved = roleRepository.save(role);
        auditLogService.record("UPDATE", "Role", saved.getId(), before, saved.getName());
        return toResponseWithPermissions(saved);
    }

    @Transactional
    public void deleteRole(UUID id) {
        Role role = getRoleById(id);
        roleRepository.delete(role);
        auditLogService.record("DELETE", "Role", id, role.getName(), null);
    }

    @Transactional
    public void assignPermission(UUID roleId, UUID permissionId) {
        Role role = getRoleById(roleId);
        Permission permission = permissionService.getPermissionById(permissionId);

        boolean alreadyAssigned = rolePermissionRepository.findAllByRoleId(roleId).stream()
                .anyMatch(rp -> rp.getPermission().getId().equals(permissionId));
        if (alreadyAssigned) {
            return;
        }

        rolePermissionRepository.save(new RolePermission(role, permission));
        auditLogService.record("ASSIGN_PERMISSION", "Role", roleId, null, permission.getCode());
    }

    @Transactional
    public void revokePermission(UUID roleId, UUID permissionId) {
        RolePermissionId id = new RolePermissionId(roleId, permissionId);
        if (rolePermissionRepository.existsById(id)) {
            rolePermissionRepository.deleteById(id);
            auditLogService.record("REVOKE_PERMISSION", "Role", roleId, permissionId, null);
        }
    }

    private RoleResponse toResponseWithPermissions(Role role) {
        RoleResponse response = roleMapper.toResponse(role);
        response.setPermissions(getPermissionsForRole(role.getId()));
        return response;
    }
}
