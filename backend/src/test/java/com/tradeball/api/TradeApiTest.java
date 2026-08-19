package com.tradeball.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeball.entity.PlayerEntity;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.PlayerStatsRepository;
import com.tradeball.repository.TradeEvaluationRepository;
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
class TradeApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PlayerRepository playerRepository;
    @Autowired PlayerStatsRepository playerStatsRepository;
    @Autowired TradeEvaluationRepository tradeEvaluationRepository;

    private String token;
    private Long starId;
    private Long roleId;

    @BeforeEach
    void setUp() throws Exception {
        tradeEvaluationRepository.deleteAll();
        playerStatsRepository.deleteAll();
        playerRepository.deleteAll();

        PlayerEntity star = TestDataFactory.player(playerRepository, "sga", "Shai", "Gilgeous-Alexander", "PG", "OKC", 27);
        PlayerEntity role = TestDataFactory.player(playerRepository, "hart", "Josh", "Hart", "SG", "NYK", 30);
        TestDataFactory.stats(playerStatsRepository, star, 2025, 32.7, 5.5, 6.4, 75);
        TestDataFactory.stats(playerStatsRepository, role, 2025, 14.0, 8.3, 5.5, 77);
        starId = star.getId();
        roleId = role.getId();

        String email = "trade" + System.nanoTime() + "@example.com";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password1!","displayName":"Trade User"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        token = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void evaluateTradeAndListHistory() throws Exception {
        mockMvc.perform(post("/api/v1/trades/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incomingPlayerIds":[%d],"outgoingPlayerIds":[%d]}
                                """.formatted(starId, roleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.verdict").isNotEmpty())
                .andExpect(jsonPath("$.categoryAnalysis").isArray())
                .andExpect(jsonPath("$.modelVersion").value("heuristic-v1-test"))
                .andExpect(jsonPath("$.tradeId").isNumber());

        mockMvc.perform(get("/api/v1/trades")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void evaluateRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/trades/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incomingPlayerIds":[%d],"outgoingPlayerIds":[%d]}
                                """.formatted(starId, roleId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateOrOverlappingPlayers() throws Exception {
        mockMvc.perform(post("/api/v1/trades/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incomingPlayerIds":[%d,%d],"outgoingPlayerIds":[%d]}
                                """.formatted(starId, starId, roleId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/trades/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incomingPlayerIds":[%d],"outgoingPlayerIds":[%d]}
                                """.formatted(starId, starId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rosterScopedTradeRequiresOutgoingPlayersOnThatRoster() throws Exception {
        MvcResult roster = mockMvc.perform(post("/api/v1/rosters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"My Team\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long rosterId = objectMapper.readTree(roster.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/rosters/{id}/players/{playerId}", rosterId, roleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/trades/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incomingPlayerIds":[%d],"outgoingPlayerIds":[%d],"rosterId":%d}
                                """.formatted(roleId, starId, rosterId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oneForThreeOfStarsIsGreatTrade() throws Exception {
        PlayerEntity star2 = TestDataFactory.player(playerRepository, "luka", "Luka", "Doncic", "PG", "LAL", 26);
        PlayerEntity star3 = TestDataFactory.player(playerRepository, "cade", "Cade", "Cunningham", "PG", "DET", 23);
        TestDataFactory.stats(playerStatsRepository, star2, 2025, 28.1, 8.7, 7.8, 64);
        TestDataFactory.stats(playerStatsRepository, star3, 2025, 22.7, 4.3, 9.9, 62);

        mockMvc.perform(post("/api/v1/trades/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"incomingPlayerIds":[%d,%d,%d],"outgoingPlayerIds":[%d]}
                                """.formatted(starId, star2.getId(), star3.getId(), roleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("GREAT_TRADE"))
                .andExpect(jsonPath("$.score").value(org.hamcrest.Matchers.greaterThanOrEqualTo(65)));
    }
}
