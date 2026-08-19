package com.tradeball.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies Flyway schema against real PostgreSQL when Docker is available.
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresFlywayIT {

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "users",
            "players",
            "player_stats",
            "rosters",
            "roster_players",
            "trade_evaluations",
            "trade_evaluation_players",
            "trade_evaluation_categories",
            "data_sync_jobs",
            "flyway_schema_history"
    );

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tradeball")
            .withUsername("tradeball")
            .withPassword("tradeball");

    @Test
    void dockerAvailableOrSkipped() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable());
        assertTrue(postgres.isRunning());
    }

    @Test
    void canConnect() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable());
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            ResultSet rs = connection.createStatement().executeQuery("SELECT 1");
            assertTrue(rs.next());
        }
    }

    @Test
    void flywayMigratesExpectedSchema() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable());
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        MigrateResult migrated = flyway.migrate();
        assertEquals("1", flyway.info().current().getVersion().getVersion());
        assertTrue(migrated.migrationsExecuted >= 0);

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            ResultSet history = connection.createStatement().executeQuery(
                    "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank");
            assertTrue(history.next());
            assertEquals("1", history.getString("version"));
            assertTrue(history.getBoolean("success"));

            ResultSet tables = connection.createStatement().executeQuery(
                    "SELECT tablename FROM pg_tables WHERE schemaname = 'public'");
            Set<String> found = new HashSet<>();
            while (tables.next()) {
                found.add(tables.getString("tablename"));
            }
            assertTrue(found.containsAll(EXPECTED_TABLES), () -> "Missing tables: " + expectedMissing(found));
        }

        MigrateResult secondRun = flyway.migrate();
        assertEquals(0, secondRun.migrationsExecuted);
    }

    private static Set<String> expectedMissing(Set<String> found) {
        Set<String> missing = new HashSet<>(EXPECTED_TABLES);
        missing.removeAll(found);
        return missing;
    }
}
