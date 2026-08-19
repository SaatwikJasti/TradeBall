package com.tradeball.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable per-player category statistics used by the fantasy engine.
 */
public final class CategoryStatistics {

    private final Map<FantasyCategory, Double> values;

    public CategoryStatistics(Map<FantasyCategory, Double> values) {
        Objects.requireNonNull(values, "values");
        EnumMap<FantasyCategory, Double> copy = new EnumMap<>(FantasyCategory.class);
        for (FantasyCategory category : FantasyCategory.values()) {
            copy.put(category, values.getOrDefault(category, 0.0));
        }
        this.values = Collections.unmodifiableMap(copy);
    }

    public double get(FantasyCategory category) {
        return values.getOrDefault(category, 0.0);
    }

    public Map<FantasyCategory, Double> asMap() {
        return values;
    }
}
