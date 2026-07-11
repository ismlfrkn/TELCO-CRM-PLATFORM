package com.turkcell.api_gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

/**
 * userKeyResolver, RequestRateLimiter'in sayim yapacagi anahtari uretir: kimligi dogrulanmis
 * istekler icin X-User-Id, aksi halde (login gibi permitAll rotalar) istemci IP'si.
 */
class RateLimiterConfigTest {

    private final KeyResolver keyResolver = new RateLimiterConfig().userKeyResolver();

    @Test
    void resolve_whenXUserIdHeaderPresent_returnsHeaderValue() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/customers")
                        .header("X-User-Id", "user-123"));

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("user-123")
                .verifyComplete();
    }

    @Test
    void resolve_whenXUserIdHeaderAbsent_fallsBackToRemoteAddress() {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.post("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("203.0.113.42", 54321));
        MockServerWebExchange exchange = MockServerWebExchange.from(requestBuilder);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("203.0.113.42")
                .verifyComplete();
    }

    @Test
    void resolve_whenXUserIdHeaderIsBlank_fallsBackToRemoteAddress() {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.get("/api/v1/orders")
                .header("X-User-Id", "   ")
                .remoteAddress(new InetSocketAddress("198.51.100.7", 8080));
        MockServerWebExchange exchange = MockServerWebExchange.from(requestBuilder);

        StepVerifier.create(keyResolver.resolve(exchange))
                .expectNext("198.51.100.7")
                .verifyComplete();
    }
}
