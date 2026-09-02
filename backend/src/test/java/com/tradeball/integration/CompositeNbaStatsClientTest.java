package com.tradeball.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tradeball.config.NbaApiProperties;
import com.tradeball.exception.ExternalApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class CompositeNbaStatsClientTest {

    @Mock HttpNbaStatsClient liveClient;
    @Mock ObjectProvider<DevFallbackNbaStatsClient> fallbackProvider;
    @Mock DevFallbackNbaStatsClient fallbackClient;

    @Test
    void usesLiveDataWhenAvailable() {
        NbaApiProperties properties = props(false);
        CompositeNbaStatsClient client = new CompositeNbaStatsClient(liveClient, fallbackProvider, properties);
        List<ExternalPlayerData> live = List.of(
                player("a"), player("b"), player("c"), player("d"), player("e"),
                player("f"), player("g"), player("h"), player("i"), player("j")
        );
        when(liveClient.fetchPlayers(2026)).thenReturn(live);

        assertEquals(10, client.fetchPlayers(2026).size());
    }

    @Test
    void doesNotUseFallbackInProductionWhenLiveFails() {
        NbaApiProperties properties = props(false);
        CompositeNbaStatsClient client = new CompositeNbaStatsClient(liveClient, fallbackProvider, properties);
        when(liveClient.fetchPlayers(2026)).thenThrow(new ExternalApiException("down"));

        assertThrows(ExternalApiException.class, () -> client.fetchPlayers(2026));
    }

    @Test
    void usesDevFallbackOnlyWhenEnabled() {
        NbaApiProperties properties = props(true);
        CompositeNbaStatsClient client = new CompositeNbaStatsClient(liveClient, fallbackProvider, properties);
        when(liveClient.fetchPlayers(2026)).thenThrow(new ExternalApiException("down"));
        when(fallbackProvider.getIfAvailable()).thenReturn(fallbackClient);
        when(fallbackClient.fetchPlayers(2026)).thenReturn(List.of(player("fallback")));

        assertEquals(1, client.fetchPlayers(2026).size());
        assertEquals("fallback", client.fetchPlayers(2026).get(0).externalId());
    }

    private static NbaApiProperties props(boolean fallback) {
        return new NbaApiProperties("https://api.server.nbaapi.com", 2026, 1000, 1000, 0, fallback);
    }

    private static ExternalPlayerData player(String id) {
        return new ExternalPlayerData(id, "First", "Last", "PG", "AAA", 25, true);
    }
}
