package com.tradeball.service;

import com.tradeball.config.FantasyScoringProperties;
import com.tradeball.domain.TradeVerdict;
import com.tradeball.util.StatMath;
import org.springframework.stereotype.Component;

/**
 * Configurable trade score policy — keeps constants out of controllers/services.
 */
@Component
public class TradeScoringPolicy {

    private final FantasyScoringProperties.Trade trade;

    public TradeScoringPolicy(FantasyScoringProperties properties) {
        this.trade = properties.getTrade();
    }

    /**
     * 9-cat trade grade using combined (summed) fantasy values, not per-player averages.
     * raw = base + (incomingSumFS - outgoingSumFS) * wFS
     *         + (outgoingAge - incomingAge) * wAge
     *         + (outgoingGP - incomingGP) * wGP
     */
    public int calculateScore(double incomingFantasyScore,
                              double outgoingFantasyScore,
                              double incomingAge,
                              double outgoingAge,
                              double incomingGamesPlayed,
                              double outgoingGamesPlayed) {
        double raw = trade.getBaseScore()
                + (incomingFantasyScore - outgoingFantasyScore) * trade.getFantasyDeltaWeight()
                + (outgoingAge - incomingAge) * trade.getAgeDeltaWeight()
                + (outgoingGamesPlayed - incomingGamesPlayed) * trade.getGamesPlayedDeltaWeight();
        return StatMath.clampRound(raw, 0, 100);
    }

    public TradeVerdict verdictFor(int score) {
        if (score >= trade.getGreatThreshold()) {
            return TradeVerdict.GREAT_TRADE;
        }
        if (score >= trade.getFairThreshold()) {
            return TradeVerdict.FAIR_TRADE;
        }
        return TradeVerdict.POOR_TRADE;
    }
}
