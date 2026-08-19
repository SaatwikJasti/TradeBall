package com.tradeball.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * League-wide population means and standard deviations for z-score calculation.
 */
public final class PopulationStats {

    private final Map<FantasyCategory, Double> means;
    private final Map<FantasyCategory, Double> stdDevs;
    private final int sampleSize;

    public PopulationStats(Map<FantasyCategory, Double> means,
                           Map<FantasyCategory, Double> stdDevs,
                           int sampleSize) {
        Objects.requireNonNull(means, "means");
        Objects.requireNonNull(stdDevs, "stdDevs");
        this.means = Collections.unmodifiableMap(new EnumMap<>(means));
        this.stdDevs = Collections.unmodifiableMap(new EnumMap<>(stdDevs));
        this.sampleSize = sampleSize;
    }

    public double mean(FantasyCategory category) {
        return means.getOrDefault(category, 0.0);
    }

    public double stdDev(FantasyCategory category) {
        double std = stdDevs.getOrDefault(category, 1.0);
        return std == 0.0 ? 1.0 : std;
    }

    public int sampleSize() {
        return sampleSize;
    }
}
