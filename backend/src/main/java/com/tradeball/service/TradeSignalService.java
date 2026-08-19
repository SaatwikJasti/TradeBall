package com.tradeball.service;

import com.tradeball.config.FantasyScoringProperties;
import com.tradeball.domain.TradeSignalType;
import com.tradeball.util.StatMath;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Isolated BUY_LOW / SELL_HIGH signal detection matching frontend heuristics.
 */
@Service
public class TradeSignalService {

    private final FantasyScoringProperties properties;

    public TradeSignalService(FantasyScoringProperties properties) {
        this.properties = properties;
    }

    public boolean isBuyLow(double incomingFantasyScore, double outgoingFantasyScore, double incomingAge) {
        return incomingFantasyScore < outgoingFantasyScore
                && incomingAge < properties.getTrade().getBuyLowMaxAge();
    }

    public boolean isSellHigh(double outgoingFantasyScore,
                              double incomingFantasyScore,
                              List<Double> populationFantasyScores) {
        List<Double> sorted = new ArrayList<>(populationFantasyScores);
        sorted.sort(Double::compareTo);
        double threshold = StatMath.percentile(sorted, properties.getTrade().getSellHighPercentile());
        return outgoingFantasyScore > threshold && incomingFantasyScore < outgoingFantasyScore;
    }

    public List<TradeSignalType> detect(double incomingFantasyScore,
                                        double outgoingFantasyScore,
                                        double incomingAge,
                                        List<Double> populationFantasyScores) {
        List<TradeSignalType> signals = new ArrayList<>();
        if (isBuyLow(incomingFantasyScore, outgoingFantasyScore, incomingAge)) {
            signals.add(TradeSignalType.BUY_LOW);
        }
        if (isSellHigh(outgoingFantasyScore, incomingFantasyScore, populationFantasyScores)) {
            signals.add(TradeSignalType.SELL_HIGH);
        }
        return signals;
    }
}
