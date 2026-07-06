package com.turkcell.identity.service;

import com.turkcell.identity.dto.request.RoleCreateRequest;
import com.turkcell.identity.dto.request.RoleUpdateRequest;
import com.turkcell.identity.dto.response.RoleResponse;
import com.turkcell.identity.entity.Permission;
import com.turkcell.identity.entity.Role;
import com.turkcell.identity.entity.RolePermission;
import com.turkcell.identity.exception.DuplicateRoleNameException;
import com.turkcell.identity.exception.RoleNotFoundException;
import com.turkcell.identity.mapper.PermissionMapper;
import com.turkcell.identity.mapper.RoleMapper;
import com.turkcell.identity.repository.RolePermissionRepository;
import com.turkcell.identity.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RoleServiceTest {

    private RoleRepository roleRepository;
    private RolePermissionRepository rolePermissionRepository;
    private PermissionService permissionService;
    private AuditLogService auditLogService;
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        rolePermissionRepository = mock(RolePermissionRepository.class);
        permissionService = mock(PermissionService.class);
        auditLogService = mock(AuditLogService.class);
        RoleMapper roleMapper = Mappers.getMapper(RoleMapper.class);
        PermissionMapper permissionMapper = Mappers.getMapper(PermissionMapper.class);

        roleService = new RoleService(roleRepository, rolePermissionRepository, permissionService,
                roleMapper, permissionMapper, auditLogService);

        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rolePermissionRepository.findAllByRoleId(any())).thenReturn(List.of());
    }

    @Test
    void createRole_withNewName_succeeds() {
        when(roleRepository.existsByName("BILLING_OPERATOR")).thenReturn(false);

        RoleCreateRequest request = new RoleCreateRequest();
        request.setName("BILLING_OPERATOR");
        request.setDescription("Bill run operator");

        RoleResponse response = roleService.createRole(request);

        assertThat(response.getName()).isEqualTo("BILLING_OPERATOR");
        verify(auditLogService).record(eq("CREATE"), eq("Role"), any(), eq(null), eq("BILLING_OPERATOR"));
    }

    @Test
    void createRole_withDuplicateName_throwsDuplicateRoleNameException() {
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        RoleCreateRequest request = new RoleCreateRequest();
        request.setName("ADMIN");

        assertThatThrownBy(() -> roleService.createRole(request)).isInstanceOf(DuplicateRoleNameException.class);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void updateRole_renamingToExistingName_throwsDuplicateRoleNameException() {
        Role role = existingRole("BILLING_OPERATOR");
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setName("ADMIN");

        assertThatThrownBy(() -> roleService.updateRole(role.getId(), request))
                .isInstanceOf(DuplicateRoleNameException.class);
    }

    @Test
    void updateRole_keepingSameName_doesNotTriggerDuplicateCheck() {
        Role role = existingRole("BILLING_OPERATOR");
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));

        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setName("BILLING_OPERATOR");
        request.setDescription("updated description");

        RoleResponse response = roleService.updateRole(role.getId(), request);

        assertThat(response.getDescription()).isEqualTo("updated description");
        verify(roleRepository, never()).existsByName(any());
    }

    @Test
    void getRoleById_whenMissing_throwsRoleNotFoundException() {
        UUID id = UUID.randomUUID();
        when(roleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleById(id)).isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void assignPermission_whenAlreadyAssigned_doesNotDuplicateOrAudit() {
        Role role = existingRole("ADMIN");
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("CUSTOMER_WRITE");

        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(permissionService.getPermissionById(permission.getId())).thenReturn(permission);
        when(rolePermissionRepository.findAllByRoleId(role.getId()))
                .thenReturn(List.of(new RolePermission(role, permission)));

        roleService.assignPermission(role.getId(), permission.getId());

        verify(rolePermissionRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void assignPermission_whenNotYetAssigned_savesAndAudits() {
        Role role = existingRole("ADMIN");
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode("CUSTOMER_WRITE");

        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(permissionService.getPermissionById(permission.getId())).thenReturn(permission);
        when(rolePermissionRepository.findAllByRoleId(role.getId())).thenReturn(List.of());

        roleService.assignPermission(role.getId(), permission.getId());

        verify(rolePermissionRepository).save(any());
        verify(auditLogService).record(eq("ASSIGN_PERMISSION"), eq("Role"), eq(role.getId()), eq(null), eq("CUSTOMER_WRITE"));
    }

    private Role existingRole(String name) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(name);
        return role;
    }
}
