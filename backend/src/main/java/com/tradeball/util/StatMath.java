package com.tradeball.util;

import java.util.List;

public final class StatMath {

    private StatMath() {
    }

    public static double mean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /** Population standard deviation; returns 1.0 when zero to avoid divide-by-zero. */
    public static double populationStdDev(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 1.0;
        }
        double mean = mean(values);
        double variance = values.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .average()
                .orElse(0.0);
        double std = Math.sqrt(variance);
        return std == 0.0 ? 1.0 : std;
    }

    public static double percentile(List<Double> sortedAscending, double percentile) {
        if (sortedAscending == null || sortedAscending.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.floor(sortedAscending.size() * percentile);
        index = Math.min(Math.max(index, 0), sortedAscending.size() - 1);
        return sortedAscending.get(index);
    }

    public static int clampRound(double value, int min, int max) {
        long rounded = Math.round(value);
        if (rounded < min) {
            return min;
        }
        if (rounded > max) {
            return max;
        }
        return (int) rounded;
    }
}
