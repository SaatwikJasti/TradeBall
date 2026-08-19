package com.tradeball.domain;

public record FantasyScore(
        double rawScore,
        Integer normalizedScore,
        CategoryZScores zScores,
        WeightedContributions contributions
) {
}
