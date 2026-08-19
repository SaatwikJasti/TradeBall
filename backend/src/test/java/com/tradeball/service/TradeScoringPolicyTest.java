package com.tradeball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tradeball.config.FantasyScoringProperties;
import com.tradeball.domain.TradeVerdict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TradeScoringPolicyTest {

    private TradeScoringPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new TradeScoringPolicy(new FantasyScoringProperties());
    }

    @Test
    void evenTradeAroundFifty() {
        int score = policy.calculateScore(5.0, 5.0, 27, 27, 70, 70);
        assertEquals(50, score);
        assertEquals(TradeVerdict.FAIR_TRADE, policy.verdictFor(score));
    }

    @Test
    void strongUpgradeIsGreatTrade() {
        int score = policy.calculateScore(10.0, 5.0, 24, 30, 75, 60);
        // fs delta 5 * 8 = 40; age (30-24)*1.2=7.2; gp (60-75)*0.3=-4.5 => 92.7
        assertEquals(93, score);
        assertEquals(TradeVerdict.GREAT_TRADE, policy.verdictFor(score));
    }

    @Test
    void downgradeIsPoorTrade() {
        int score = policy.calculateScore(2.0, 8.0, 30, 24, 50, 75);
        assertTruePoor(score);
    }

    @Test
    void clampsToZeroAndHundred() {
        assertEquals(0, policy.calculateScore(-50, 50, 40, 20, 10, 80));
        assertEquals(100, policy.calculateScore(50, -50, 18, 40, 82, 10));
    }

    @Test
    void verdictThresholds() {
        assertEquals(TradeVerdict.GREAT_TRADE, policy.verdictFor(65));
        assertEquals(TradeVerdict.FAIR_TRADE, policy.verdictFor(45));
        assertEquals(TradeVerdict.POOR_TRADE, policy.verdictFor(44));
    }

    private void assertTruePoor(int score) {
        assertEquals(TradeVerdict.POOR_TRADE, policy.verdictFor(score));
        org.junit.jupiter.api.Assertions.assertTrue(score < 45);
    }
}
