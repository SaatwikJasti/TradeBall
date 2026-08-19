package com.tradeball.config;

import com.tradeball.domain.FantasyCategory;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tradeball.fantasy")
public class FantasyScoringProperties {

    private String modelVersion = "heuristic-v1";
    private Map<FantasyCategory, Double> weights = defaultWeights();
    private Trade trade = new Trade();

    private static Map<FantasyCategory, Double> defaultWeights() {
        EnumMap<FantasyCategory, Double> map = new EnumMap<>(FantasyCategory.class);
        for (FantasyCategory category : FantasyCategory.values()) {
            map.put(category, category.defaultWeight());
        }
        return map;
    }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Map<FantasyCategory, Double> getWeights() { return weights; }
    public void setWeights(Map<FantasyCategory, Double> weights) { this.weights = weights; }
    public Trade getTrade() { return trade; }
    public void setTrade(Trade trade) { this.trade = trade; }

    public double weight(FantasyCategory category) {
        return weights.getOrDefault(category, category.defaultWeight());
    }

    public static class Trade {
        private double baseScore = 50;
        private double fantasyDeltaWeight = 8.0;
        private double ageDeltaWeight = 1.2;
        private double gamesPlayedDeltaWeight = 0.3;
        private int greatThreshold = 65;
        private int fairThreshold = 45;
        private int buyLowMaxAge = 26;
        private double sellHighPercentile = 0.75;

        public double getBaseScore() { return baseScore; }
        public void setBaseScore(double baseScore) { this.baseScore = baseScore; }
        public double getFantasyDeltaWeight() { return fantasyDeltaWeight; }
        public void setFantasyDeltaWeight(double fantasyDeltaWeight) { this.fantasyDeltaWeight = fantasyDeltaWeight; }
        public double getAgeDeltaWeight() { return ageDeltaWeight; }
        public void setAgeDeltaWeight(double ageDeltaWeight) { this.ageDeltaWeight = ageDeltaWeight; }
        public double getGamesPlayedDeltaWeight() { return gamesPlayedDeltaWeight; }
        public void setGamesPlayedDeltaWeight(double gamesPlayedDeltaWeight) { this.gamesPlayedDeltaWeight = gamesPlayedDeltaWeight; }
        public int getGreatThreshold() { return greatThreshold; }
        public void setGreatThreshold(int greatThreshold) { this.greatThreshold = greatThreshold; }
        public int getFairThreshold() { return fairThreshold; }
        public void setFairThreshold(int fairThreshold) { this.fairThreshold = fairThreshold; }
        public int getBuyLowMaxAge() { return buyLowMaxAge; }
        public void setBuyLowMaxAge(int buyLowMaxAge) { this.buyLowMaxAge = buyLowMaxAge; }
        public double getSellHighPercentile() { return sellHighPercentile; }
        public void setSellHighPercentile(double sellHighPercentile) { this.sellHighPercentile = sellHighPercentile; }
    }
}
