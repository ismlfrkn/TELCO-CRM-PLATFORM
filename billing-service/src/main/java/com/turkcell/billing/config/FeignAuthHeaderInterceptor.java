package com.turkcell.billing.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * billing-service, otomatik bill-run icin subscription-service ve product-catalog-service'i
 * doğrudan (gateway'i atlayarak, Eureka uzerinden) cagirir. Ama o servislerin
 * GatewayHeaderAuthenticationFilter'i sadece X-Internal-Gateway-Secret header'i dogruysa X-User-Id'ye
 * guvenir - yani billing-service de tipki gateway gibi bu secret'i bilmek ve iletmek zorunda, aksi
 * halde tum downstream cagrilar 401 doner (bkz. order-service/config/FeignAuthHeaderInterceptor.java
 * - ayni desenin ilk implementasyonu, bu servise kopyalanmamisti).
 */
@Configuration
public class FeignAuthHeaderInterceptor {

    private final GatewayTrustProperties gatewayTrustProperties;

    public FeignAuthHeaderInterceptor(GatewayTrustProperties gatewayTrustProperties) {
        this.gatewayTrustProperties = gatewayTrustProperties;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate template) -> {
            template.header("X-Internal-Gateway-Secret", gatewayTrustProperties.getInternalSecret());
            currentUserId().ifPresent(userId -> template.header("X-User-Id", userId));
        };
    }

    private java.util.Optional<String> currentUserId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes) {
            return java.util.Optional.ofNullable(servletRequestAttributes.getRequest().getHeader("X-User-Id"));
        }
        return java.util.Optional.empty();
    }
}
