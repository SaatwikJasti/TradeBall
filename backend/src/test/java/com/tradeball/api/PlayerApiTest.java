package com.tradeball.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tradeball.entity.PlayerEntity;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.PlayerStatsRepository;
import com.tradeball.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlayerApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired PlayerRepository playerRepository;
    @Autowired PlayerStatsRepository playerStatsRepository;

    private Long playerId;

    @BeforeEach
    void seed() {
        playerStatsRepository.deleteAll();
        playerRepository.deleteAll();
        PlayerEntity a = TestDataFactory.player(playerRepository, "jokic", "Nikola", "Jokic", "C", "DEN", 30);
        PlayerEntity b = TestDataFactory.player(playerRepository, "curry", "Stephen", "Curry", "PG", "GSW", 37);
        TestDataFactory.stats(playerStatsRepository, a, 2025, 29.6, 12.7, 10.0, 79);
        TestDataFactory.stats(playerStatsRepository, b, 2025, 22.5, 3.9, 6.1, 66);
        playerId = a.getId();
    }

    @Test
    void listsAndSearchesPlayers() throws Exception {
        mockMvc.perform(get("/api/v1/players?page=0&size=10&sort=lastName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/v1/players/search").param("q", "jokic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastName").value("Jokic"));

        mockMvc.perform(get("/api/v1/players/{id}", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nikola"));

        mockMvc.perform(get("/api/v1/players/{id}/stats", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(29.6));

        mockMvc.perform(get("/api/v1/players/{id}/fantasy-value", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fantasyScore").isNumber())
                .andExpect(jsonPath("$.modelVersion").isNotEmpty());
    }
}
