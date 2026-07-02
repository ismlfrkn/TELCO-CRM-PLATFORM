package com.turkcell.identity.service;

import com.turkcell.identity.dto.request.PatchUserRequest;
import com.turkcell.identity.dto.request.RegisterUserRequest;
import com.turkcell.identity.dto.request.UpdateUserRequest;
import com.turkcell.identity.dto.response.RoleResponse;
import com.turkcell.identity.dto.response.UserResponse;
import com.turkcell.identity.entity.Role;
import com.turkcell.identity.entity.User;
import com.turkcell.identity.entity.UserRole;
import com.turkcell.identity.entity.UserRoleId;
import com.turkcell.identity.exception.DuplicateEmailException;
import com.turkcell.identity.exception.DuplicateUsernameException;
import com.turkcell.identity.exception.UserNotFoundException;
import com.turkcell.identity.mapper.RoleMapper;
import com.turkcell.identity.mapper.UserMapper;
import com.turkcell.identity.repository.UserRepository;
import com.turkcell.identity.repository.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository, RoleService roleService,
                        PasswordEncoder passwordEncoder, UserMapper userMapper, RoleMapper roleMapper,
                        AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.auditLogService = auditLogService;
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllByStatusNot("INACTIVE", pageable).map(this::toResponseWithRoles);
    }

    public UserResponse getUserResponseById(UUID id) {
        return toResponseWithRoles(getUserById(id));
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public List<RoleResponse> getRolesForUser(UUID userId) {
        return userRoleRepository.findAllByUserId(userId).stream()
                .map(UserRole::getRole)
                .map(role -> {
                    RoleResponse response = roleMapper.toResponse(role);
                    response.setPermissions(roleService.getPermissionsForRole(role.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<String> getRoleNamesForUser(UUID userId) {
        return userRoleRepository.findAllByUserId(userId).stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse registerUser(RegisterUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException("Username already in use: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already in use: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCustomerId(request.getCustomerId());
        user.setStatus("ACTIVE");

        User saved = userRepository.save(user);
        auditLogService.record("CREATE", "User", saved.getId(), null, auditSnapshot(saved));
        return toResponseWithRoles(saved);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = getUserById(id);
        String before = auditSnapshot(user);

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already in use: " + request.getEmail());
        }

        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(request.getStatus());
        user.setCustomerId(request.getCustomerId());

        User saved = userRepository.save(user);
        auditLogService.record("UPDATE", "User", saved.getId(), before, auditSnapshot(saved));
        return toResponseWithRoles(saved);
    }

    @Transactional
    public UserResponse patchUser(UUID id, PatchUserRequest request) {
        User user = getUserById(id);
        String before = auditSnapshot(user);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateEmailException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getCustomerId() != null) {
            user.setCustomerId(request.getCustomerId());
        }

        User saved = userRepository.save(user);
        auditLogService.record("UPDATE", "User", saved.getId(), before, auditSnapshot(saved));
        return toResponseWithRoles(saved);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = getUserById(id);
        String before = auditSnapshot(user);
        user.setStatus("INACTIVE");
        User saved = userRepository.save(user);
        auditLogService.record("DELETE", "User", id, before, auditSnapshot(saved));
    }

    @Transactional
    public void recordLogin(UUID userId) {
        User user = getUserById(userId);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void assignRole(UUID userId, UUID roleId) {
        User user = getUserById(userId);
        Role role = roleService.getRoleById(roleId);

        boolean alreadyAssigned = userRoleRepository.findAllByUserId(userId).stream()
                .anyMatch(ur -> ur.getRole().getId().equals(roleId));
        if (alreadyAssigned) {
            return;
        }

        userRoleRepository.save(new UserRole(user, role));
        auditLogService.record("ASSIGN_ROLE", "User", userId, null, role.getName());
    }

    @Transactional
    public void revokeRole(UUID userId, UUID roleId) {
        UserRoleId id = new UserRoleId(userId, roleId);
        if (userRoleRepository.existsById(id)) {
            userRoleRepository.deleteById(id);
            auditLogService.record("REVOKE_ROLE", "User", userId, roleId, null);
        }
    }

    private UserResponse toResponseWithRoles(User user) {
        UserResponse response = userMapper.toResponse(user);
        response.setRoles(getRolesForUser(user.getId()));
        return response;
    }

    private String auditSnapshot(User user) {
        return user.getUsername() + "|" + user.getEmail() + "|" + user.getPhoneNumber() + "|" + user.getStatus();
    }
}
