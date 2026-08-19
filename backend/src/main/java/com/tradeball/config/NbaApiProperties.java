package com.tradeball.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tradeball.nba")
public record NbaApiProperties(
        String baseUrl,
        int season,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxRetries,
        boolean useDevFallback
) {
}
