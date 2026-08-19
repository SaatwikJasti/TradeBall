package com.tradeball.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradeball.config.NbaApiProperties;
import com.tradeball.exception.ExternalApiException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Live NBA stats from api.server.nbaapi.com. Season totals are converted to per-game averages.
 */
@Component
public class HttpNbaStatsClient {

    private static final Logger log = LoggerFactory.getLogger(HttpNbaStatsClient.class);
    private static final int PAGE_SIZE = 100;
    private static final int MIN_GAMES = 10;

    private final RestTemplate restTemplate;
    private final NbaApiProperties properties;
    private final Object cacheLock = new Object();
    private int cachedSeason = Integer.MIN_VALUE;
    private List<JsonNode> cachedRows = List.of();

    public HttpNbaStatsClient(RestTemplateBuilder builder, NbaApiProperties properties) {
        this.properties = properties;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()))
                .build();
    }

    public List<ExternalPlayerData> fetchPlayers(int season) {
        List<ExternalPlayerData> players = new ArrayList<>();
        for (JsonNode node : regularSeasonTotals(season)) {
            String name = text(node, "playerName");
            if (name == null) {
                continue;
            }
            String[] parts = splitName(asciiFold(name));
            players.add(new ExternalPlayerData(
                    externalId(node, name),
                    parts[0],
                    parts[1],
                    text(node, "position"),
                    text(node, "team"),
                    intOrNull(node, "age"),
                    true
            ));
        }
        return players;
    }

    public List<ExternalPlayerStatsData> fetchPlayerStats(int season) {
        List<ExternalPlayerStatsData> result = new ArrayList<>();
        for (JsonNode node : regularSeasonTotals(season)) {
            String name = text(node, "playerName");
            int games = intVal(node, "games", 0);
            if (name == null || games < MIN_GAMES) {
                continue;
            }
            result.add(new ExternalPlayerStatsData(
                    externalId(node, name),
                    season,
                    games,
                    perGame(dbl(node, "points"), games),
                    perGame(dbl(node, "totalRb"), games),
                    perGame(dbl(node, "assists"), games),
                    perGame(dbl(node, "steals"), games),
                    perGame(dbl(node, "blocks"), games),
                    perGame(dbl(node, "threeFg"), games),
                    asPercent(dbl(node, "fieldPercent")),
                    asPercent(dbl(node, "ftPercent")),
                    perGame(dbl(node, "turnovers"), games)
            ));
        }
        if (result.size() < 10) {
            throw new ExternalApiException("NBA API returned insufficient player stats");
        }
        return result;
    }

    private List<JsonNode> regularSeasonTotals(int season) {
        synchronized (cacheLock) {
            if (season == cachedSeason && !cachedRows.isEmpty()) {
                return cachedRows;
            }
            Map<String, JsonNode> byPlayer = new LinkedHashMap<>();
            int page = 1;
            int pages = 1;
            do {
                JsonNode body = getPage("/api/playertotals", season, "points", page);
                JsonNode data = body.path("data");
                if (!data.isArray()) {
                    throw new ExternalApiException("Invalid NBA player totals response");
                }
                for (JsonNode node : data) {
                    if (node.path("isPlayoff").asBoolean(false)) {
                        continue;
                    }
                    int games = intVal(node, "games", 0);
                    if (games < MIN_GAMES) {
                        continue;
                    }
                    String key = externalId(node, text(node, "playerName"));
                    JsonNode existing = byPlayer.get(key);
                    if (existing == null || games > intVal(existing, "games", 0)) {
                        byPlayer.put(key, node);
                    }
                }
                JsonNode pagination = body.path("pagination");
                pages = pagination.path("pages").asInt(pagination.path("totalPages").asInt(page));
                page++;
            } while (page <= pages);
            cachedRows = List.copyOf(byPlayer.values());
            cachedSeason = season;
            log.info("Loaded {} regular-season NBA player rows for season {}", cachedRows.size(), season);
            return cachedRows;
        }
    }

    private JsonNode getPage(String path, int season, String sortBy, int page) {
        int attempts = Math.max(1, properties.maxRetries() + 1);
        RestClientException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                String url = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + path)
                        .queryParam("page", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .queryParam("sortBy", sortBy)
                        .queryParam("ascending", false)
                        .queryParam("season", season)
                        .queryParam("isPlayoff", false)
                        .toUriString();
                ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    throw new ExternalApiException("NBA API non-success status: " + response.getStatusCode());
                }
                return response.getBody();
            } catch (RestClientException ex) {
                last = ex;
                log.warn("NBA API call failed attempt={} path={} page={} msg={}", i + 1, path, page, ex.getMessage());
            }
        }
        throw new ExternalApiException("NBA API unavailable after retries", last);
    }

    private static String externalId(JsonNode node, String name) {
        String playerId = text(node, "playerId");
        if (playerId != null && !playerId.isBlank()) {
            return playerId.trim().toLowerCase(Locale.US);
        }
        return slug(name);
    }

    private static String[] splitName(String full) {
        String trimmed = full.trim();
        int idx = trimmed.lastIndexOf(' ');
        if (idx < 0) {
            return new String[]{trimmed, ""};
        }
        return new String[]{trimmed.substring(0, idx), trimmed.substring(idx + 1)};
    }

    private static String asciiFold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private static String slug(String name) {
        return asciiFold(name).trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.get(field);
        return child == null || child.isNull() || child.asText().isBlank() ? null : child.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        return child.asInt();
    }

    private static int intVal(JsonNode node, String field, int fallback) {
        JsonNode child = node.get(field);
        return child == null || child.isNull() ? fallback : child.asInt(fallback);
    }

    private static double dbl(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child == null || child.isNull() ? 0.0 : child.asDouble(0.0);
    }

    private static double perGame(double total, int games) {
        if (games <= 0) {
            return 0.0;
        }
        return round1(total / games);
    }

    private static double asPercent(double value) {
        double pct = value <= 1.0 ? value * 100.0 : value;
        return round1(pct);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
