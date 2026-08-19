package com.tradeball.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DEVELOPMENT-ONLY fallback data sourced from the TradeBall frontend curated snapshot.
 * Enabled when tradeball.nba.use-dev-fallback=true (default for local/dev).
 */
@Component
@ConditionalOnProperty(name = "tradeball.nba.use-dev-fallback", havingValue = "true")
public class DevFallbackNbaStatsClient implements NbaStatsClient {

    private static final Logger log = LoggerFactory.getLogger(DevFallbackNbaStatsClient.class);

    private static final Object[][] ROWS = {
            {"LeBron James", "LAL", "SF", 40, 71, 23.7, 8.3, 9.0, 1.3, 0.6, 1.6, 53.9, 74.1, 3.5},
            {"Stephen Curry", "GSW", "PG", 37, 66, 22.5, 3.9, 6.1, 1.2, 0.3, 4.5, 45.0, 92.3, 2.8},
            {"Nikola Jokic", "DEN", "C", 30, 79, 29.6, 12.7, 10.0, 1.9, 0.9, 1.2, 57.5, 81.7, 4.0},
            {"Giannis Antetokounmpo", "MIL", "PF", 30, 73, 30.4, 11.9, 6.5, 1.2, 1.1, 0.7, 61.2, 68.4, 3.5},
            {"Luka Doncic", "LAL", "PG", 26, 55, 28.1, 8.7, 7.8, 1.4, 0.5, 3.4, 47.9, 77.0, 4.0},
            {"Jayson Tatum", "BOS", "SF", 27, 75, 26.9, 8.1, 4.9, 1.1, 0.6, 2.9, 47.1, 83.0, 2.6},
            {"Joel Embiid", "PHI", "C", 31, 39, 24.6, 11.5, 5.7, 1.0, 1.7, 0.7, 52.8, 82.5, 3.4},
            {"Kevin Durant", "PHX", "SF", 36, 65, 27.1, 6.6, 5.0, 0.9, 1.2, 2.3, 52.5, 85.0, 3.0},
            {"Damian Lillard", "MIL", "PG", 34, 73, 24.3, 4.4, 7.0, 0.9, 0.3, 4.2, 43.5, 91.1, 3.3},
            {"Anthony Davis", "LAL", "PF", 32, 76, 24.7, 12.6, 3.5, 1.2, 2.3, 0.4, 55.6, 78.5, 2.4},
            {"Devin Booker", "PHX", "SG", 28, 76, 25.3, 4.2, 6.5, 1.0, 0.3, 2.8, 49.0, 89.3, 3.4},
            {"Trae Young", "ATL", "PG", 26, 74, 25.7, 3.3, 10.8, 0.8, 0.1, 2.9, 42.9, 87.4, 4.4},
            {"Ja Morant", "MEM", "PG", 25, 67, 22.1, 5.8, 8.7, 1.0, 0.4, 1.3, 47.1, 78.0, 3.3},
            {"Jaylen Brown", "BOS", "SG", 28, 70, 23.0, 5.5, 3.6, 1.1, 0.5, 2.4, 49.3, 76.8, 2.0},
            {"Zion Williamson", "NOP", "PF", 24, 68, 22.9, 5.8, 4.7, 1.0, 0.7, 0.3, 56.0, 70.0, 2.8},
            {"Bam Adebayo", "MIA", "C", 27, 71, 19.3, 10.4, 3.8, 1.0, 0.9, 0.1, 55.3, 73.4, 2.2},
            {"Karl-Anthony Towns", "NYK", "C", 29, 76, 25.1, 13.7, 3.1, 1.0, 0.6, 3.0, 50.3, 83.5, 3.3},
            {"Pascal Siakam", "IND", "PF", 31, 72, 21.3, 7.8, 4.6, 0.9, 0.6, 1.1, 47.9, 79.1, 2.4},
            {"Tyrese Haliburton", "IND", "PG", 25, 69, 20.1, 3.9, 10.9, 1.6, 0.3, 3.0, 47.7, 88.6, 3.9},
            {"Anthony Edwards", "MIN", "SG", 23, 78, 25.9, 5.4, 5.1, 1.3, 0.5, 3.1, 46.1, 83.7, 3.1},
            {"Cade Cunningham", "DET", "PG", 23, 62, 22.7, 4.3, 9.9, 1.0, 0.4, 2.6, 43.6, 82.3, 3.4},
            {"De'Aaron Fox", "SAS", "PG", 27, 74, 25.2, 4.3, 6.2, 1.6, 0.3, 1.7, 50.1, 79.0, 3.1},
            {"Donovan Mitchell", "CLE", "SG", 28, 74, 26.6, 5.1, 6.1, 1.8, 0.4, 3.4, 47.3, 84.4, 2.9},
            {"Shai Gilgeous-Alexander", "OKC", "PG", 27, 75, 32.7, 5.5, 6.4, 2.0, 1.0, 2.2, 53.5, 90.5, 2.4},
            {"Kawhi Leonard", "LAC", "SF", 33, 68, 23.7, 6.1, 3.6, 1.9, 0.8, 2.0, 52.5, 87.8, 2.1},
            {"Jimmy Butler", "GSW", "SF", 35, 60, 18.3, 4.5, 5.0, 1.4, 0.4, 0.3, 48.6, 83.2, 2.6},
            {"Draymond Green", "GSW", "PF", 35, 72, 8.5, 7.2, 6.2, 1.4, 0.8, 0.6, 46.8, 66.0, 3.0},
            {"Victor Wembanyama", "SAS", "C", 21, 71, 24.0, 10.6, 3.9, 1.4, 3.6, 2.1, 46.5, 79.0, 3.1},
            {"Paolo Banchero", "ORL", "PF", 22, 73, 22.6, 6.9, 5.4, 1.0, 0.7, 1.8, 46.4, 73.0, 3.2},
            {"Scottie Barnes", "TOR", "SF", 24, 75, 21.5, 8.9, 6.1, 1.4, 0.8, 1.5, 48.8, 70.0, 2.4},
            {"Evan Mobley", "CLE", "C", 23, 78, 18.5, 9.9, 3.2, 1.2, 1.7, 0.5, 56.7, 75.0, 1.9},
            {"Alperen Sengun", "HOU", "C", 22, 72, 21.1, 9.6, 5.6, 1.0, 1.4, 0.3, 53.5, 76.0, 3.2},
            {"Franz Wagner", "ORL", "SF", 23, 76, 24.0, 5.1, 4.2, 1.1, 0.5, 2.0, 48.7, 79.0, 2.0},
            {"Tyrese Maxey", "PHI", "PG", 24, 70, 25.9, 3.7, 6.4, 1.1, 0.4, 3.1, 47.4, 86.9, 2.5},
            {"Jalen Brunson", "NYK", "PG", 28, 77, 27.3, 3.4, 6.8, 0.9, 0.2, 2.4, 47.9, 85.7, 2.5},
            {"Anthony Edwards", "MIN", "SG", 23, 78, 25.9, 5.4, 5.1, 1.3, 0.5, 3.1, 46.1, 83.7, 3.1},
            {"Jalen Johnson", "ATL", "PF", 23, 73, 21.8, 9.4, 5.8, 1.5, 0.8, 1.4, 51.7, 79.0, 2.6},
            {"Darius Garland", "CLE", "PG", 25, 60, 20.6, 2.7, 7.8, 1.1, 0.2, 2.8, 45.5, 87.0, 3.0},
            {"Josh Hart", "NYK", "SG", 30, 77, 14.0, 8.3, 5.5, 1.7, 0.4, 1.7, 49.2, 71.0, 2.1},
            {"Rudy Gobert", "MIN", "C", 32, 73, 13.4, 12.9, 1.3, 0.8, 2.1, 0.0, 67.0, 61.0, 1.7},
    };

    @Override
    public List<ExternalPlayerData> fetchPlayers(int season) {
        log.warn("DEVELOPMENT-ONLY NbaStatsClient in use (curated fallback snapshot)");
        List<ExternalPlayerData> players = new ArrayList<>();
        for (Object[] row : ROWS) {
            String name = (String) row[0];
            String[] parts = splitName(name);
            players.add(new ExternalPlayerData(
                    slug(name),
                    parts[0],
                    parts[1],
                    (String) row[2],
                    (String) row[1],
                    (Integer) row[3],
                    true
            ));
        }
        return players;
    }

    @Override
    public List<ExternalPlayerStatsData> fetchPlayerStats(int season) {
        log.warn("DEVELOPMENT-ONLY NbaStatsClient stats path in use");
        List<ExternalPlayerStatsData> stats = new ArrayList<>();
        for (Object[] row : ROWS) {
            String name = (String) row[0];
            stats.add(new ExternalPlayerStatsData(
                    slug(name),
                    season,
                    (Integer) row[4],
                    ((Number) row[5]).doubleValue(),
                    ((Number) row[6]).doubleValue(),
                    ((Number) row[7]).doubleValue(),
                    ((Number) row[8]).doubleValue(),
                    ((Number) row[9]).doubleValue(),
                    ((Number) row[10]).doubleValue(),
                    ((Number) row[11]).doubleValue(),
                    ((Number) row[12]).doubleValue(),
                    ((Number) row[13]).doubleValue()
            ));
        }
        return stats;
    }

    private static String[] splitName(String full) {
        int idx = full.lastIndexOf(' ');
        if (idx < 0) {
            return new String[]{full, ""};
        }
        return new String[]{full.substring(0, idx), full.substring(idx + 1)};
    }

    private static String slug(String name) {
        return name.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-");
    }
}
