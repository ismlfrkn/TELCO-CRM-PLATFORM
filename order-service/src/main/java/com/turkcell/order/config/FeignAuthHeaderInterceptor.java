package com.turkcell.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
