package com.tradeball.integration;

public record ExternalPlayerStatsData(
        String externalId,
        Integer season,
        Integer gamesPlayed,
        Double points,
        Double rebounds,
        Double assists,
        Double steals,
        Double blocks,
        Double threePointers,
        Double fieldGoalPercentage,
        Double freeThrowPercentage,
        Double turnovers
) {
}
