package com.tradeball.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class CategoryZScores {

    private final Map<FantasyCategory, Double> zScores;

    public CategoryZScores(Map<FantasyCategory, Double> zScores) {
        Objects.requireNonNull(zScores, "zScores");
        EnumMap<FantasyCategory, Double> copy = new EnumMap<>(FantasyCategory.class);
        copy.putAll(zScores);
        this.zScores = Collections.unmodifiableMap(copy);
    }

    public double get(FantasyCategory category) {
        return zScores.getOrDefault(category, 0.0);
    }

    public Map<FantasyCategory, Double> asMap() {
        return zScores;
    }
}
