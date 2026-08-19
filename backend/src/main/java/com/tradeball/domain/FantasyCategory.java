package com.tradeball.domain;

/**
 * Nine-category fantasy basketball scoring, matching the TradeBall frontend model.
 */
public enum FantasyCategory {
    PTS(1.00),
    REB(0.85),
    AST(0.90),
    STL(1.20),
    BLK(1.10),
    THREE_PM(0.80),
    FG_PCT(0.70),
    FT_PCT(0.50),
    TO(-1.00);

    private final double defaultWeight;

    FantasyCategory(double defaultWeight) {
        this.defaultWeight = defaultWeight;
    }

    public double defaultWeight() {
        return defaultWeight;
    }

    public boolean isPercentage() {
        return this == FG_PCT || this == FT_PCT;
    }
}
