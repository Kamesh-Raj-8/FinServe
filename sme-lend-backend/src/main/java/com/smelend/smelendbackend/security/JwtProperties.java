package com.smelend.smelendbackend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long expMinutes;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpMinutes() {
        return expMinutes;
    }

    public void setExpMinutes(long expMinutes) {
        this.expMinutes = expMinutes;
    }
}