package com.tradeball.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tradeball.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
}
