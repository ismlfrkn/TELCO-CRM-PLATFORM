package com.turkcell.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gateway ile downstream servisler arasinda paylasilan internal secret.
 * Not: sinif adi bilerek "GatewayProperties" DEGIL - Spring Cloud Gateway'in kendi
 * org.springframework.cloud.gateway.config.GatewayProperties auto-configuration bean'i
 * ile "gatewayProperties" bean adi cakismasini onlemek icin GatewayTrustProperties kullanildi.
 */
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayTrustProperties {

    private String internalSecret;

    public String getInternalSecret() { return internalSecret; }
    public void setInternalSecret(String internalSecret) { this.internalSecret = internalSecret; }
}
