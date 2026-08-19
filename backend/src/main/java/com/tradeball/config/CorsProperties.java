package com.tradeball.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tradeball.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
