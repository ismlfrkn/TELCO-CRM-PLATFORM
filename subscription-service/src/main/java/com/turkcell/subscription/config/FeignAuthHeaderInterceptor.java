package com.turkcell.subscription.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * subscription-service, PaymentCompleted tuketildiginde tarife versiyonu icin
 * product-catalog-service'i doğrudan (gateway'i atlayarak, Eureka uzerinden) cagirir. Ama o
 * servisin GatewayHeaderAuthenticationFilter'i sadece X-Internal-Gateway-Secret header'i
 * dogruysa X-User-Id'ye guvenir - order-service/billing-service/usage-service'teki ayni desen.
 * Bu cagri bir Kafka consumer'dan tetiklendigi icin HTTP request context'i yok
 * (RequestContextHolder null doner) - bu durumda "system" sabit degerine dusulur, aksi halde
 * product-catalog-service X-User-Id bos oldugu icin 401 doner (usage-service'te ayni bug canli
 * testte bulunup duzeltilmisti, bkz. CLAUDE.md Bolum 16).
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

    private java.util.Optional<String> currentUserId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes) {
            return java.util.Optional.ofNullable(servletRequestAttributes.getRequest().getHeader("X-User-Id"));
        }
        return java.util.Optional.empty();
    }
}
