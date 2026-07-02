package com.turkcell.identity.security;

import com.turkcell.identity.config.JwtProperties;
import com.turkcell.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(jwtProperties.getSecret()));
    }

    public String generateAccessToken(User user, List<String> roleNames) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLES, roleNames)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    public Instant getExpiration(Claims claims) {
        return claims.getExpiration().toInstant();
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpiration() / 1000;
    }
}
