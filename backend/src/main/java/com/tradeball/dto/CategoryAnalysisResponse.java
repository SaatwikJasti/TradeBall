package com.tradeball.dto;

import com.tradeball.domain.CategoryImpact;
import com.tradeball.domain.FantasyCategory;

public record CategoryAnalysisResponse(
        FantasyCategory category,
        Double incomingValue,
        Double outgoingValue,
        Double delta,
        Double zScoreDelta,
        CategoryImpact impact
) {
}
