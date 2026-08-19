package com.tradeball.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeball.entity.PlayerEntity;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RosterApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PlayerRepository playerRepository;

    private String token;
    private Long playerId;

    @BeforeEach
    void setUp() throws Exception {
        playerRepository.deleteAll();
        PlayerEntity player = TestDataFactory.player(playerRepository, "tatum", "Jayson", "Tatum", "SF", "BOS", 27);
        playerId = player.getId();

        String email = "roster" + System.nanoTime() + "@example.com";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password1!","displayName":"Roster User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void rosterCrudAndAuthorization() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/rosters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"My Team\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Team"))
                .andReturn();
        Long rosterId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/rosters/{id}/players/{playerId}", rosterId, playerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.length()").value(1));

        mockMvc.perform(post("/api/v1/rosters/{id}/players/{playerId}", rosterId, playerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/rosters/{id}", rosterId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/rosters/{id}", rosterId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));

        mockMvc.perform(delete("/api/v1/rosters/{id}/players/{playerId}", rosterId, playerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.length()").value(0));

        String otherEmail = "other" + System.nanoTime() + "@example.com";
        MvcResult other = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password1!","displayName":"Other"}
                                """.formatted(otherEmail)))
                .andReturn();
        String otherToken = objectMapper.readTree(other.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/rosters/{id}", rosterId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/rosters/{id}", rosterId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void unauthenticatedRosterAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/rosters")).andExpect(status().isUnauthorized());
    }
}
