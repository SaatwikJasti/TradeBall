package com.tradeball.integration;

import com.tradeball.config.NbaApiProperties;
import com.tradeball.exception.ExternalApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Prefers live NBA stats. Uses the curated snapshot only when explicitly enabled and live fetch fails.
 */
@Component
@Primary
public class CompositeNbaStatsClient implements NbaStatsClient {

    private static final Logger log = LoggerFactory.getLogger(CompositeNbaStatsClient.class);

    private final HttpNbaStatsClient liveClient;
    private final ObjectProvider<DevFallbackNbaStatsClient> fallbackClient;
    private final NbaApiProperties properties;

    public CompositeNbaStatsClient(HttpNbaStatsClient liveClient,
                                   ObjectProvider<DevFallbackNbaStatsClient> fallbackClient,
                                   NbaApiProperties properties) {
        this.liveClient = liveClient;
        this.fallbackClient = fallbackClient;
        this.properties = properties;
    }

    @Override
    public List<ExternalPlayerData> fetchPlayers(int season) {
        try {
            List<ExternalPlayerData> live = liveClient.fetchPlayers(season);
            if (live.size() >= 10) {
                return live;
            }
            throw new ExternalApiException("Live NBA player payload was too small");
        } catch (RuntimeException ex) {
            DevFallbackNbaStatsClient fallback = fallback();
            if (fallback == null) {
                throw wrap(ex);
            }
            log.warn("Live NBA players fetch failed ({}). Using development snapshot.", ex.getMessage());
            return fallback.fetchPlayers(season);
        }
    }

    @Override
    public List<ExternalPlayerStatsData> fetchPlayerStats(int season) {
        try {
            return liveClient.fetchPlayerStats(season);
        } catch (RuntimeException ex) {
            DevFallbackNbaStatsClient fallback = fallback();
            if (fallback == null) {
                throw wrap(ex);
            }
            log.warn("Live NBA stats fetch failed ({}). Using development snapshot.", ex.getMessage());
            return fallback.fetchPlayerStats(season);
        }
    }

    private DevFallbackNbaStatsClient fallback() {
        return properties.useDevFallback() ? fallbackClient.getIfAvailable() : null;
    }

    private static ExternalApiException wrap(RuntimeException ex) {
        if (ex instanceof ExternalApiException api) {
            return api;
        }
        return new ExternalApiException("NBA API unavailable", ex);
    }
}
