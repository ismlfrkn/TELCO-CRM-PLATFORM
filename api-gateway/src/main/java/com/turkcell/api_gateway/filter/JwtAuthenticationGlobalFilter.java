package com.turkcell.api_gateway.filter;

import com.turkcell.api_gateway.config.GatewayTrustProperties;
import com.turkcell.api_gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gateway'e gelen her istegi (auth/actuator disinda) Authorization: Bearer <token> ile dogrular,
 * gecerliyse token claim'lerinden turetilen X-User-Id / X-User-Roles header'larini (ve downstream
 * servislerin bu header'lara guvenmesi icin X-Internal-Gateway-Secret'i) enjekte eder.
 * Boylece downstream servisler JWT'yi tekrar dogrulamaz, sadece gateway'e (bu secret uzerinden) guvenir.
 *
 * WebFlux'ta MDC (thread-local) guvenilir calismadigi icin correlationId burada identity-service'teki
 * CorrelationIdFilter ile ayni Correlation-Id header'indan okunur/uretilir ve downstream'e aynen
 * iletilir - boylece tek bir istegin gateway'deki ve servisteki loglari/hata gövdeleri ayni id ile
 * eslesir (dogrudan §12.1 formatindaki instance/correlationId alanlari icin de kullanilir).
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("/api/v1/auth/", "/actuator/");
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Gateway-Secret";
    private static final String CORRELATION_ID_HEADER = "Correlation-Id";

    private final GatewayTrustProperties gatewayTrustProperties;
    private final SecretKey signingKey;

    public JwtAuthenticationGlobalFilter(JwtProperties jwtProperties, GatewayTrustProperties gatewayTrustProperties) {
        this.gatewayTrustProperties = gatewayTrustProperties;
        this.signingKey = Keys.hmacShaKeyFor(HexFormat.of().parseHex(jwtProperties.getSecret()));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = resolveCorrelationId(exchange);

        if (isPublicPath(path)) {
            // Kimligi dogrulanmamis istekler de gecebilir; yine de client'in gateway-guveni
            // header'larini forge etmesini engellemek icin bu header'lari temizliyoruz.
            ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                    .headers(httpHeaders -> {
                        stripTrustHeaders(httpHeaders);
                        httpHeaders.set(CORRELATION_ID_HEADER, correlationId);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(sanitizedRequest).build());
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, path, correlationId, "Missing or malformed Authorization header");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorized(exchange, path, correlationId, "Invalid or expired access token");
        }

        if (!"access".equals(claims.get("type", String.class))) {
            return unauthorized(exchange, path, correlationId, "Provided token is not an access token");
        }

        String userId = claims.getSubject();
        String rolesHeaderValue = extractRoles(claims);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    stripTrustHeaders(httpHeaders);
                    httpHeaders.set(USER_ID_HEADER, userId);
                    httpHeaders.set(USER_ROLES_HEADER, rolesHeaderValue);
                    httpHeaders.set(INTERNAL_SECRET_HEADER, gatewayTrustProperties.getInternalSecret());
                    httpHeaders.set(CORRELATION_ID_HEADER, correlationId);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void stripTrustHeaders(org.springframework.http.HttpHeaders httpHeaders) {
        httpHeaders.remove(USER_ID_HEADER);
        httpHeaders.remove(USER_ROLES_HEADER);
        httpHeaders.remove(INTERNAL_SECRET_HEADER);
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String existing = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        return (existing == null || existing.isBlank()) ? UUID.randomUUID().toString() : existing;
    }

    private String extractRoles(Claims claims) {
        List<?> rawRoles = claims.get("roles", List.class);
        if (rawRoles == null) {
            return "";
        }
        return rawRoles.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String path, String correlationId, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        String body = String.format(
                "{\"type\":\"https://telco.example/errors/unauthorized\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"%s\",\"instance\":\"%s\",\"correlationId\":\"%s\"}",
                escapeJson(detail), escapeJson(path), escapeJson(correlationId)
        );
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
