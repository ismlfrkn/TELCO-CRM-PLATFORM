package com.turkcell.identity.service;

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
import io.jsonwebtoken.JwtException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final String BLACKLIST_KEY_PREFIX = "refresh-token-blacklist:";

    private final UserRepository userRepository;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository, UserService userService, RefreshTokenRepository refreshTokenRepository,
                        JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder, StringRedisTemplate redisTemplate,
                        AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username/email or password"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new InvalidCredentialsException("Account is not active");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        AuthResponse authResponse = issueTokens(user);
        userService.recordLogin(user.getId());
        auditLogService.record(user.getId(), "LOGIN", "User", user.getId(), null, null);
        return authResponse;
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims = parseOrThrow(request.getRefreshToken());
        UUID userId = jwtTokenProvider.getUserId(claims);
        String jti = jwtTokenProvider.getJti(claims);

        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti))) {
            revokeAllRefreshTokens(userId);
            auditLogService.record(userId, "REFRESH_TOKEN_REUSE_DETECTED", "User", userId, null, null);
            throw new InvalidRefreshTokenException("Refresh token reuse detected, all sessions have been revoked");
        }

        String tokenHash = sha256(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            revokeAllRefreshTokens(userId);
            auditLogService.record(userId, "REFRESH_TOKEN_REUSE_DETECTED", "User", userId, null, null);
            throw new InvalidRefreshTokenException("Refresh token reuse detected, all sessions have been revoked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found for refresh token"));

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
        blacklistJti(jti, jwtTokenProvider.getExpiration(claims));

        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        try {
            Claims claims = parseOrThrow(request.getRefreshToken());
            String jti = jwtTokenProvider.getJti(claims);
            UUID userId = jwtTokenProvider.getUserId(claims);

            refreshTokenRepository.findByTokenHash(sha256(request.getRefreshToken()))
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
            blacklistJti(jti, jwtTokenProvider.getExpiration(claims));
            auditLogService.record(userId, "LOGOUT", "User", userId, null, null);
        } catch (InvalidRefreshTokenException ex) {

        }
    }

    private AuthResponse issueTokens(User user) {
        List<String> roleNames = userService.getRoleNamesForUser(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(user, roleNames);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setUserId(user.getId());
        tokenEntity.setTokenHash(sha256(refreshToken));
        tokenEntity.setExpiresAt(jwtTokenProvider.getExpiration(refreshClaims));
        tokenEntity.setRevoked(false);
        refreshTokenRepository.save(tokenEntity);

        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtTokenProvider.getAccessTokenExpirationSeconds());
    }

    private void revokeAllRefreshTokens(UUID userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        for (RefreshToken token : activeTokens) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    private void blacklistJti(String jti, Instant expiresAt) {
        long ttlSeconds = Math.max(1, Duration.between(Instant.now(), expiresAt).getSeconds());
        redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "revoked", ttlSeconds, TimeUnit.SECONDS);
    }

    private Claims parseOrThrow(String token) {
        try {
            return jwtTokenProvider.parseClaims(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
