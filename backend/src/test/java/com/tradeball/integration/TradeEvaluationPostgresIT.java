package com.tradeball.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Real PostgreSQL API checks; opt-in benchmark shares exactly the same fixture. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class TradeEvaluationPostgresIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.cache.type", () -> "none");
        properties.add("spring.jpa.properties.hibernate.cache.use_second_level_cache", () -> "false");
        properties.add("spring.jpa.properties.hibernate.jdbc.batch_size", () -> "0");
        properties.add("spring.jpa.properties.hibernate.session_factory.statement_inspector", SqlProbe.class::getName);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    String token;
    String otherToken;

    @BeforeEach
    void fixture() throws Exception {
        jdbc.execute("TRUNCATE users, players, player_stats, rosters, roster_players, trade_evaluations, "
                + "trade_evaluation_players, trade_evaluation_categories RESTART IDENTITY CASCADE");
        // SQL generation is deterministic; setup is never inside the measured request.
        jdbc.execute("""
                INSERT INTO players(id, external_id, first_name, last_name, position, team, age, active)
                SELECT i, 'fixture-' || i, 'Player', lpad(i::text, 3, '0'), 'PG', 'FIX', 20 + i % 16, true
                FROM generate_series(1, 500) i
                """);
        jdbc.execute("""
                INSERT INTO player_stats(player_id, season, games_played, points, rebounds, assists,
                    steals, blocks, three_pointers, field_goal_percentage, free_throw_percentage, turnovers)
                SELECT i, 2025, 40 + i % 43, 8 + i % 30, 2 + i % 12, 1 + i % 10,
                    0.4 + (i % 16) * 0.1, 0.2 + (i % 20) * 0.1, 0.5 + (i % 35) * 0.1,
                    38 + i % 25, 60 + i % 36, 0.5 + (i % 30) * 0.1
                FROM generate_series(1, 500) i
                """);
        // Off-season rows expose accidental omission of the season filter.
        jdbc.execute("""
                INSERT INTO player_stats(player_id, season, games_played, points)
                SELECT i, 2024, 82, 999 FROM generate_series(1, 500) i
                """);
        token = register("owner@fixture.local");
        otherToken = register("other@fixture.local");
    }

    String register(String email) throws Exception {
        MvcResult response = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email,
                                "password", "Password1!", "displayName", "Fixture"))))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).path("accessToken").asText();
    }

    String trade(int size) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "incomingPlayerIds", IntStream.rangeClosed(1, size).boxed().toList(),
                "outgoingPlayerIds", IntStream.rangeClosed(101, 100 + size).boxed().toList()));
    }

    MvcResult evaluate(String body, String bearer) throws Exception {
        return mvc.perform(post("/api/v1/trades/evaluate").header("Authorization", "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    JsonNode json(MvcResult response) throws Exception {
        return mapper.readTree(response.getResponse().getContentAsString());
    }

    void clearHistory() {
        jdbc.execute("TRUNCATE trade_evaluations, trade_evaluation_players, trade_evaluation_categories RESTART IDENTITY CASCADE");
    }

    @Test
    void valuationHistoryValidationAndOwnership() throws Exception {
        // Non-sorted IDs ensure batching cannot silently reorder players and their scores.
        String body = "{\"incomingPlayerIds\":[3,1],\"outgoingPlayerIds\":[102,101]}";
        MvcResult response = evaluate(body, token);
        assertEquals(200, response.getResponse().getStatus());
        JsonNode result = json(response);
        assertEquals(9, result.path("categoryAnalysis").size());
        assertEquals("heuristic-v1-test", result.path("modelVersion").asText());
        assertTrue(result.path("incomingFantasyScore").isNumber());
        long id = result.path("tradeId").asLong();
        MvcResult history = mvc.perform(get("/api/v1/trades/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(result.path("score").asInt()))
                .andExpect(jsonPath("$.incomingFantasyScore").value(result.path("incomingFantasyScore").asDouble()))
                .andExpect(jsonPath("$.outgoingFantasyScore").value(result.path("outgoingFantasyScore").asDouble()))
                .andReturn();
        assertEquals(result.path("categoryAnalysis"), json(history).path("categoryAnalysis"));
        mvc.perform(get("/api/v1/trades/{id}", id).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/trades").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        for (String invalid : List.of(
                "{\"incomingPlayerIds\":[null],\"outgoingPlayerIds\":[101]}",
                "{\"incomingPlayerIds\":[1,1],\"outgoingPlayerIds\":[101]}",
                "{\"incomingPlayerIds\":[1],\"outgoingPlayerIds\":[1]}")) {
            assertEquals(400, evaluate(invalid, token).getResponse().getStatus());
        }
        assertEquals(404, evaluate("{\"incomingPlayerIds\":[501],\"outgoingPlayerIds\":[101]}", token)
                .getResponse().getStatus());
        jdbc.update("DELETE FROM player_stats WHERE player_id=1 AND season=2025");
        MvcResult missingStats = evaluate(trade(1), token);
        assertEquals(404, missingStats.getResponse().getStatus());
        assertTrue(json(missingStats).path("message").asText().contains("Stats not found"));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM trade_evaluations", Integer.class));

        MvcResult roster = mvc.perform(post("/api/v1/rosters").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Fixture roster\"}"))
                .andExpect(status().isCreated()).andReturn();
        long rosterId = json(roster).path("id").asLong();
        mvc.perform(post("/api/v1/rosters/{id}/players/101", rosterId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        String scoped = "{\"incomingPlayerIds\":[2],\"outgoingPlayerIds\":[101],\"rosterId\":" + rosterId + "}";
        assertEquals(403, evaluate(scoped, otherToken).getResponse().getStatus());
        assertEquals(400, evaluate(scoped.replace("[101]", "[102]"), token).getResponse().getStatus());
        assertEquals(200, evaluate(scoped, token).getResponse().getStatus());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM trade_evaluations", Integer.class));
        mvc.perform(post("/api/v1/trades/evaluate").contentType(MediaType.APPLICATION_JSON).content(trade(1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @EnabledIfSystemProperty(named = "tradeball.benchmark", matches = "true")
    void benchmark() throws Exception {
        List<Map<String, Object>> samples = new ArrayList<>();
        Map<String, JsonNode> snapshots = new LinkedHashMap<>();
        Map<String, List<String>> sqlExamples = new LinkedHashMap<>();
        for (int round = 0; round < 2; round++) {
            for (int size : (round == 0 ? new int[]{1, 3, 10} : new int[]{10, 3, 1})) {
                String body = trade(size);
                for (int warm = 0; warm < 30; warm++) {
                    clearHistory();
                    assertEquals(200, evaluate(body, token).getResponse().getStatus());
                }
                for (int sample = 0; sample < 100; sample++) {
                    clearHistory();
                    SqlProbe.begin();
                    MvcResult response;
                    long start = System.nanoTime();
                    try {
                        response = evaluate(body, token);
                    } finally {
                        SqlProbe.end();
                    }
                    long elapsed = System.nanoTime() - start;
                    List<String> sql = SqlProbe.statements();
                    long selects = sql.stream().filter(s -> s.startsWith("select")).count();
                    long inserts = sql.stream().filter(s -> s.startsWith("insert")).count();
                    int status = response.getResponse().getStatus();
                    samples.add(Map.of("round", round + 1, "playersPerSide", size,
                            "sample", sample, "latencyMs", elapsed / 1_000_000.0,
                            "status", status, "statements", sql.size(), "selects", selects, "inserts", inserts));
                    if (status == 200) {
                        ObjectNode snapshot = (ObjectNode) json(response);
                        snapshot.remove("tradeId");
                        JsonNode previous = snapshots.putIfAbsent(Integer.toString(size), snapshot);
                        if (previous != null) assertEquals(previous, snapshot, "Nondeterministic valuation");
                    }
                    sqlExamples.putIfAbsent(Integer.toString(size), sql);
                }
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("fixture", "v1: 500 active players, 500 season-2025 rows, 500 off-season-2024 rows, 2 users");
        report.put("java", System.getProperty("java.runtime.version"));
        report.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        report.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        report.put("postgres", jdbc.queryForObject("SELECT version()", String.class));
        report.put("image", POSTGRES.getDockerImageName());
        report.put("concurrency", 1);
        report.put("warmupPerScenarioPerRound", 30);
        report.put("samplesPerScenarioPerRound", 100);
        report.put("rounds", 2);
        report.put("cache", "Spring none, Hibernate L2 disabled, new transaction/persistence context each request; warmed DB/OS buffers");
        report.put("transport", "MockMvc authenticated POST /api/v1/trades/evaluate; includes commit and serialization; excludes socket/network");
        report.put("snapshots", snapshots);
        report.put("sqlExamples", sqlExamples);
        report.put("samples", samples);
        Path output = Path.of(System.getProperty("tradeball.benchmark.output", "target/trade-benchmark.json"));
        Files.createDirectories(output.toAbsolutePath().getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
        String baseline = System.getProperty("tradeball.benchmark.baseline");
        if (baseline != null) {
            assertEquals(mapper.readTree(Path.of(baseline).toFile()).path("snapshots"),
                    mapper.valueToTree(snapshots), "Valuation changed from baseline");
        }
        assertTrue(samples.stream().allMatch(s -> s.get("status").equals(200)), "Non-200 responses; inspect raw report");
    }

    /** Request-thread-only SQL preparation counts; cleanup SQL is deliberately excluded. */
    public static class SqlProbe implements StatementInspector {
        private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
        private static final ThreadLocal<List<String>> SQL = ThreadLocal.withInitial(ArrayList::new);
        static void begin() { SQL.get().clear(); ACTIVE.set(true); }
        static void end() { ACTIVE.set(false); }
        static List<String> statements() { return List.copyOf(SQL.get()); }
        @Override public String inspect(String sql) {
            if (ACTIVE.get()) SQL.get().add(sql.stripLeading().toLowerCase(Locale.ROOT));
            return sql;
        }
    }
}
