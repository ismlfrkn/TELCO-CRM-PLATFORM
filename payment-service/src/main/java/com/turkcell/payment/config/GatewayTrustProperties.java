package com.turkcell.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * api-gateway ile paylasilan internal secret. GatewayHeaderAuthenticationFilter, X-User-Id/X-User-Roles
 * header'larina yalnizca istekte bu secret dogru degerde geldiyse guvenir (identity-service ile ayni pattern).
 */
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayTrustProperties {

    private String internalSecret;

    public String getInternalSecret() { return internalSecret; }
    public void setInternalSecret(String internalSecret) { this.internalSecret = internalSecret; }
}
