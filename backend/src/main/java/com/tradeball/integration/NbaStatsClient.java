package com.tradeball.integration;

import java.util.List;

/**
 * Abstraction over the external NBA stats provider.
 * Controllers must never call the remote API directly.
 */
public interface NbaStatsClient {
    List<ExternalPlayerData> fetchPlayers(int season);
    List<ExternalPlayerStatsData> fetchPlayerStats(int season);
}
