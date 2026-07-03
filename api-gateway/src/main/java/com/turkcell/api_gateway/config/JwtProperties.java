package com.turkcell.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * identity-service'in access token uretmek icin kullandigi ayni HMAC secret'i tasir;
 * gateway bu secret ile token'i tekrar imzalamadan sadece dogrular.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
