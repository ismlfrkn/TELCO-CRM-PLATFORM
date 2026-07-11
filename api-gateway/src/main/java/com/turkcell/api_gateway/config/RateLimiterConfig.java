package com.turkcell.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Bolum 12: RequestRateLimiter'in hangi "kullaniciya" gore sayim yapacagini belirler.
 * JwtAuthenticationGlobalFilter (order=-100, bu filtreden once calisir) kimligi dogrulanmis
 * isteklere X-User-Id header'ini enjekte eder; o header varsa oradan, yoksa (ör. /api/v1/auth/**
 * gibi permitAll rotalar) istemci IP adresinden anahtar uretilir - boylece login denemeleri de
 * rate limit'e tabi olur, sadece kullanici bazinda degil IP bazinda.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }

            String remoteAddress = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(remoteAddress);
        };
    }
}
