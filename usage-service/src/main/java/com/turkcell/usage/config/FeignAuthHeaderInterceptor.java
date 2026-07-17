package com.turkcell.usage.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * usage-service, SubscriptionActivated tuketildiginde tarife bilgisi icin product-catalog-service'i
 * doğrudan (gateway'i atlayarak, Eureka uzerinden) cagirir. Ama o servisin
 * GatewayHeaderAuthenticationFilter'i sadece X-Internal-Gateway-Secret header'i dogruysa X-User-Id'ye
 * guvenir - order-service/billing-service'teki ayni desen (bkz. CLAUDE.md Bolum 16 - bu interceptor
 * eksikse cagrilar sessizce 401 doner, sadece canlı ortamda ortaya cikar).
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
            template.header("X-User-Id", currentUserId().orElse("system"));
        };
    }

    /**
     * order-service/billing-service'teki orijinal desen sadece HTTP-istek-tetikledigi Feign
     * cagrilarini varsayiyordu (gelen X-User-Id'yi ileri tasir). usage-service'te bu interceptor
     * Kafka consumer'dan (SubscriptionEventConsumerConfig) da cagriliyor - orada hicbir HTTP
     * request context'i yok, RequestContextHolder null doner. product-catalog-service'in
     * GatewayHeaderAuthenticationFilter'i X-User-Id'nin bos OLMAMASINI sart kosuyor (secret dogru
     * olsa bile) - bu yuzden HTTP context yoksa "system" sabit degerine dusuyoruz, aksi halde
     * event-tetiklemeli cagrilar sessizce 401 alir (canli testte tam olarak boyle bulundu).
     */
    private java.util.Optional<String> currentUserId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes) {
            return java.util.Optional.ofNullable(servletRequestAttributes.getRequest().getHeader("X-User-Id"));
        }
        return java.util.Optional.empty();
    }
}
