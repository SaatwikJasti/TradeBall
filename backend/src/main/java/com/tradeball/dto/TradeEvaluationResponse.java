package com.tradeball.dto;

import com.tradeball.domain.TradeVerdict;
import java.util.List;

public record TradeEvaluationResponse(
        Long tradeId,
        Integer score,
        TradeVerdict verdict,
        Double incomingFantasyScore,
        Double outgoingFantasyScore,
        List<CategoryAnalysisResponse> categoryAnalysis,
        List<String> strengths,
        List<String> weaknesses,
        List<String> signals,
        String explanation,
        String modelVersion
) {
}
