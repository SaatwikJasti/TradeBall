package com.tradeball.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Marker documentation: Redis can be disabled with tradeball.redis.enabled=false.
 * When Redis auto-config is excluded / connection factory missing, LocalCacheFallbackConfig applies.
 */
@Configuration
@ConditionalOnProperty(name = "tradeball.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisOptionalAutoConfig {
}
