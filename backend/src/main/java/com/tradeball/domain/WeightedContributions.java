package com.tradeball.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class WeightedContributions {

    private final Map<FantasyCategory, Double> contributions;
    private final double total;

    public WeightedContributions(Map<FantasyCategory, Double> contributions) {
        Objects.requireNonNull(contributions, "contributions");
        EnumMap<FantasyCategory, Double> copy = new EnumMap<>(FantasyCategory.class);
        copy.putAll(contributions);
        this.contributions = Collections.unmodifiableMap(copy);
        this.total = copy.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double get(FantasyCategory category) {
        return contributions.getOrDefault(category, 0.0);
    }

    public double total() {
        return total;
    }

    public Map<FantasyCategory, Double> asMap() {
        return contributions;
    }
}
