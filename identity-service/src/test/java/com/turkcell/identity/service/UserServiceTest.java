package com.turkcell.identity.service;

import com.turkcell.identity.dto.request.RegisterUserRequest;
import com.turkcell.identity.dto.request.UpdateUserRequest;
import com.turkcell.identity.dto.response.UserResponse;
import com.turkcell.identity.entity.Role;
import com.turkcell.identity.entity.User;
import com.turkcell.identity.entity.UserRole;
import com.turkcell.identity.exception.DuplicateEmailException;
import com.turkcell.identity.exception.DuplicateUsernameException;
import com.turkcell.identity.exception.UserNotFoundException;
import com.turkcell.identity.mapper.RoleMapper;
import com.turkcell.identity.mapper.UserMapper;
import com.turkcell.identity.repository.UserRepository;
import com.turkcell.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RoleService roleService;
    private PasswordEncoder passwordEncoder;
    private AuditLogService auditLogService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        roleService = mock(RoleService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditLogService = mock(AuditLogService.class);
        UserMapper userMapper = Mappers.getMapper(UserMapper.class);
        RoleMapper roleMapper = Mappers.getMapper(RoleMapper.class);

        userService = new UserService(userRepository, userRoleRepository, roleService,
                passwordEncoder, userMapper, roleMapper, auditLogService);

        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRoleRepository.findAllByUserId(any())).thenReturn(List.of());
    }

    @Test
    void registerUser_withNewUsernameAndEmail_createsActiveUser() {
        when(userRepository.existsByUsername("serhat")).thenReturn(false);
        when(userRepository.existsByEmail("serhat@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("hashed");

        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername("serhat");
        request.setEmail("serhat@example.com");
        request.setPhoneNumber("5551234567");
        request.setPassword("Passw0rd!");

        UserResponse response = userService.registerUser(request);

        assertThat(response.getUsername()).isEqualTo("serhat");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(auditLogService).record(eq("CREATE"), eq("User"), any(), eq(null), any());
    }

    @Test
    void registerUser_withDuplicateUsername_throwsDuplicateUsernameException() {
        when(userRepository.existsByUsername("serhat")).thenReturn(true);

        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername("serhat");
        request.setEmail("new@example.com");
        request.setPhoneNumber("5551234567");
        request.setPassword("Passw0rd!");

        assertThatThrownBy(() -> userService.registerUser(request)).isInstanceOf(DuplicateUsernameException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_withDuplicateEmail_throwsDuplicateEmailException() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        RegisterUserRequest request = new RegisterUserRequest();
        request.setUsername("newuser");
        request.setEmail("taken@example.com");
        request.setPhoneNumber("5551234567");
        request.setPassword("Passw0rd!");

        assertThatThrownBy(() -> userService.registerUser(request)).isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateUser_changingToTakenEmail_throwsDuplicateEmailException() {
        User user = existingUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("someoneelse@example.com")).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("someoneelse@example.com");
        request.setPhoneNumber("5551234567");
        request.setStatus("ACTIVE");

        assertThatThrownBy(() -> userService.updateUser(user.getId(), request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateUser_keepingSameEmail_doesNotTriggerDuplicateCheck() {
        User user = existingUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail(user.getEmail());
        request.setPhoneNumber("5559999999");
        request.setStatus("ACTIVE");

        UserResponse response = userService.updateUser(user.getId(), request);

        assertThat(response.getPhoneNumber()).isEqualTo("5559999999");
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void deleteUser_setsStatusToInactive() {
        User user = existingUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.deleteUser(user.getId());

        assertThat(user.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void getUserById_whenMissing_throwsUserNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void assignRole_whenAlreadyAssigned_doesNotDuplicateOrAudit() {
        User user = existingUser();
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("ADMIN");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleService.getRoleById(role.getId())).thenReturn(role);
        when(userRoleRepository.findAllByUserId(user.getId()))
                .thenReturn(List.of(new UserRole(user, role)));

        userService.assignRole(user.getId(), role.getId());

        verify(userRoleRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void assignRole_whenNotYetAssigned_savesAndAudits() {
        User user = existingUser();
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("ADMIN");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleService.getRoleById(role.getId())).thenReturn(role);
        when(userRoleRepository.findAllByUserId(user.getId())).thenReturn(List.of());

        userService.assignRole(user.getId(), role.getId());

        verify(userRoleRepository).save(any());
        verify(auditLogService).record(eq("ASSIGN_ROLE"), eq("User"), eq(user.getId()), eq(null), eq("ADMIN"));
    }

    private User existingUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("serhat");
        user.setEmail("serhat@example.com");
        user.setPhoneNumber("5551234567");
        user.setPasswordHash("hashed");
        user.setStatus("ACTIVE");
        return user;
    }
}
