package com.turkcell.identity.service;

import com.turkcell.identity.dto.request.PermissionCreateRequest;
import com.turkcell.identity.dto.request.PermissionUpdateRequest;
import com.turkcell.identity.dto.response.PermissionResponse;
import com.turkcell.identity.entity.Permission;
import com.turkcell.identity.exception.DuplicatePermissionCodeException;
import com.turkcell.identity.exception.PermissionNotFoundException;
import com.turkcell.identity.mapper.PermissionMapper;
import com.turkcell.identity.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PermissionServiceTest {

    private PermissionRepository permissionRepository;
    private AuditLogService auditLogService;
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionRepository = mock(PermissionRepository.class);
        auditLogService = mock(AuditLogService.class);
        PermissionMapper permissionMapper = Mappers.getMapper(PermissionMapper.class);

        permissionService = new PermissionService(permissionRepository, permissionMapper, auditLogService);

        when(permissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createPermission_withNewCode_succeeds() {
        when(permissionRepository.existsByCode("CUSTOMER_WRITE")).thenReturn(false);

        PermissionCreateRequest request = new PermissionCreateRequest();
        request.setCode("CUSTOMER_WRITE");
        request.setDescription("Create/update customers");

        PermissionResponse response = permissionService.createPermission(request);

        assertThat(response.getCode()).isEqualTo("CUSTOMER_WRITE");
        verify(auditLogService).record(eq("CREATE"), eq("Permission"), any(), eq(null), eq("CUSTOMER_WRITE"));
    }

    @Test
    void createPermission_withDuplicateCode_throwsDuplicatePermissionCodeException() {
        when(permissionRepository.existsByCode("CUSTOMER_WRITE")).thenReturn(true);

        PermissionCreateRequest request = new PermissionCreateRequest();
        request.setCode("CUSTOMER_WRITE");

        assertThatThrownBy(() -> permissionService.createPermission(request))
                .isInstanceOf(DuplicatePermissionCodeException.class);
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void updatePermission_changingToTakenCode_throwsDuplicatePermissionCodeException() {
        Permission permission = existingPermission("CUSTOMER_WRITE");
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));
        when(permissionRepository.existsByCode("CUSTOMER_READ")).thenReturn(true);

        PermissionUpdateRequest request = new PermissionUpdateRequest();
        request.setCode("CUSTOMER_READ");

        assertThatThrownBy(() -> permissionService.updatePermission(permission.getId(), request))
                .isInstanceOf(DuplicatePermissionCodeException.class);
    }

    @Test
    void updatePermission_keepingSameCode_doesNotTriggerDuplicateCheck() {
        Permission permission = existingPermission("CUSTOMER_WRITE");
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));

        PermissionUpdateRequest request = new PermissionUpdateRequest();
        request.setCode("CUSTOMER_WRITE");
        request.setDescription("updated");

        PermissionResponse response = permissionService.updatePermission(permission.getId(), request);

        assertThat(response.getDescription()).isEqualTo("updated");
        verify(permissionRepository, never()).existsByCode(any());
    }

    @Test
    void getPermissionById_whenMissing_throwsPermissionNotFoundException() {
        UUID id = UUID.randomUUID();
        when(permissionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.getPermissionById(id))
                .isInstanceOf(PermissionNotFoundException.class);
    }

    @Test
    void deletePermission_removesAndAudits() {
        Permission permission = existingPermission("CUSTOMER_WRITE");
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));

        permissionService.deletePermission(permission.getId());

        verify(permissionRepository).delete(permission);
        verify(auditLogService).record(eq("DELETE"), eq("Permission"), eq(permission.getId()), eq("CUSTOMER_WRITE"), eq(null));
    }

    private Permission existingPermission(String code) {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode(code);
        return permission;
    }
}
