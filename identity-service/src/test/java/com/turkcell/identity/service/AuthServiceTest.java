package com.turkcell.identity.service;

import com.turkcell.identity.config.JwtProperties;
import com.turkcell.identity.dto.request.LoginRequest;
import com.turkcell.identity.dto.request.RefreshRequest;
import com.turkcell.identity.dto.response.AuthResponse;
import com.turkcell.identity.entity.RefreshToken;
import com.turkcell.identity.entity.User;
import com.turkcell.identity.exception.InvalidCredentialsException;
import com.turkcell.identity.exception.InvalidRefreshTokenException;
import com.turkcell.identity.repository.RefreshTokenRepository;
import com.turkcell.identity.repository.UserRepository;
import com.turkcell.identity.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private UserService userService;
    private RefreshTokenRepository refreshTokenRepository;
    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private AuditLogService auditLogService;
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = mock(UserService.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        auditLogService = mock(AuditLogService.class);

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtProperties.setAccessTokenExpiration(900_000);
        jwtProperties.setRefreshTokenExpiration(86_400_000);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService = new AuthService(userRepository, userService, refreshTokenRepository,
                jwtTokenProvider, passwordEncoder, redisTemplate, auditLogService);

        activeUser = new User();
        activeUser.setId(UUID.randomUUID());
        activeUser.setUsername("serhat");
        activeUser.setEmail("serhat@example.com");
        activeUser.setPasswordHash("hashed");
        activeUser.setStatus("ACTIVE");

        when(userService.getRoleNamesForUser(activeUser.getId())).thenReturn(List.of("CUSTOMER"));
    }

    @Test
    void login_withValidCredentials_returnsTokensAndRecordsLogin() {
        when(userRepository.findByUsername("serhat")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correct-password", "hashed")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("serhat");
        request.setPassword("correct-password");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        verify(userService).recordLogin(activeUser.getId());
        verify(auditLogService).record(eq(activeUser.getId()), eq("LOGIN"), eq("User"), eq(activeUser.getId()), isNull(), isNull());
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentialsException() {
        when(userRepository.findByUsername("serhat")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("serhat");
        request.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_withInactiveAccount_throwsInvalidCredentialsException() {
        activeUser.setStatus("SUSPENDED");
        when(userRepository.findByUsername("serhat")).thenReturn(Optional.of(activeUser));

        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("serhat");
        request.setPassword("whatever");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_withUnknownUsernameOrEmail_throwsInvalidCredentialsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("ghost");
        request.setPassword("whatever");

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_withValidToken_rotatesTokenAndBlacklistsOldOne() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(activeUser);
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String jti = jwtTokenProvider.getJti(claims);

        RefreshToken stored = new RefreshToken();
        stored.setUserId(activeUser.getId());
        stored.setTokenHash(sha256(refreshToken));
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        stored.setRevoked(false);

        when(redisTemplate.hasKey("refresh-token-blacklist:" + jti)).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(sha256(refreshToken))).thenReturn(Optional.of(stored));
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        AuthResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotEqualTo(refreshToken);
        assertThat(stored.isRevoked()).isTrue();
        verify(valueOperations).set(eq("refresh-token-blacklist:" + jti), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void refresh_withBlacklistedJti_detectsReuseAndRevokesAllSessions() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(activeUser);
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String jti = jwtTokenProvider.getJti(claims);

        when(redisTemplate.hasKey("refresh-token-blacklist:" + jti)).thenReturn(true);
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(activeUser.getId()))
                .thenReturn(List.of(activeSessionToken()));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        assertThatThrownBy(() -> authService.refresh(request)).isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).saveAll(argThat(tokens -> {
            for (RefreshToken t : tokens) {
                if (!t.isRevoked()) return false;
            }
            return true;
        }));
        verify(auditLogService).record(eq(activeUser.getId()), eq("REFRESH_TOKEN_REUSE_DETECTED"), eq("User"), eq(activeUser.getId()), isNull(), isNull());
    }

    @Test
    void refresh_withAlreadyRevokedStoredToken_detectsReuseAndThrows() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(activeUser);
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String jti = jwtTokenProvider.getJti(claims);

        RefreshToken stored = new RefreshToken();
        stored.setUserId(activeUser.getId());
        stored.setTokenHash(sha256(refreshToken));
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        stored.setRevoked(true);

        when(redisTemplate.hasKey("refresh-token-blacklist:" + jti)).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(sha256(refreshToken))).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(activeUser.getId())).thenReturn(List.of());

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        assertThatThrownBy(() -> authService.refresh(request)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_withUnrecognizedTokenHash_throwsInvalidRefreshTokenException() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(activeUser);
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String jti = jwtTokenProvider.getJti(claims);

        when(redisTemplate.hasKey("refresh-token-blacklist:" + jti)).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(sha256(refreshToken))).thenReturn(Optional.empty());

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        assertThatThrownBy(() -> authService.refresh(request)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logout_revokesStoredTokenAndBlacklistsJti() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(activeUser);
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String jti = jwtTokenProvider.getJti(claims);

        RefreshToken stored = new RefreshToken();
        stored.setUserId(activeUser.getId());
        stored.setTokenHash(sha256(refreshToken));
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        stored.setRevoked(false);
        when(refreshTokenRepository.findByTokenHash(sha256(refreshToken))).thenReturn(Optional.of(stored));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        authService.logout(request);

        assertThat(stored.isRevoked()).isTrue();
        verify(valueOperations).set(eq("refresh-token-blacklist:" + jti), eq("revoked"), anyLong(), eq(TimeUnit.SECONDS));
        verify(auditLogService).record(eq(activeUser.getId()), eq("LOGOUT"), eq("User"), eq(activeUser.getId()), isNull(), isNull());
    }

    @Test
    void logout_withGarbageToken_isIdempotentAndDoesNotThrow() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("not-a-real-jwt");

        authService.logout(request);

        verifyNoInteractions(auditLogService);
    }

    private RefreshToken activeSessionToken() {
        RefreshToken token = new RefreshToken();
        token.setUserId(activeUser.getId());
        token.setRevoked(false);
        return token;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
