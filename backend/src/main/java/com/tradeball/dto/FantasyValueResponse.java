package com.tradeball.dto;

import java.util.Map;

public record FantasyValueResponse(
        Long playerId,
        String playerName,
        Integer season,
        Double fantasyScore,
        Integer normalizedScore,
        Map<String, Double> categoryZScores,
        Map<String, Double> weightedContributions,
        String modelVersion
) {
}
