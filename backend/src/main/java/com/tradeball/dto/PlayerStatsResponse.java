package com.tradeball.dto;

public record PlayerStatsResponse(
        Long playerId,
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
