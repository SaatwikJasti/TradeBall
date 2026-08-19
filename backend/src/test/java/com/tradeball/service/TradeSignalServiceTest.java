package com.tradeball.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tradeball.config.FantasyScoringProperties;
import com.tradeball.domain.TradeSignalType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TradeSignalServiceTest {

    private TradeSignalService service;

    @BeforeEach
    void setUp() {
        service = new TradeSignalService(new FantasyScoringProperties());
    }

    @Test
    void detectsBuyLow() {
        assertTrue(service.isBuyLow(3.0, 5.0, 22));
        assertFalse(service.isBuyLow(3.0, 5.0, 28));
        assertFalse(service.isBuyLow(6.0, 5.0, 22));
    }

    @Test
    void detectsSellHigh() {
        List<Double> population = List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0);
        // 75th percentile index floor(8*0.75)=6 -> 7.0
        assertTrue(service.isSellHigh(7.5, 5.0, population));
        assertFalse(service.isSellHigh(6.0, 5.0, population));
        assertFalse(service.isSellHigh(8.0, 9.0, population));
    }

    @Test
    void detectReturnsBothWhenApplicable() {
        List<Double> population = List.of(0.0, 1.0, 2.0, 3.0, 10.0);
        List<TradeSignalType> signals = service.detect(2.0, 9.0, 21, population);
        assertTrue(signals.contains(TradeSignalType.BUY_LOW));
        assertTrue(signals.contains(TradeSignalType.SELL_HIGH));
    }
}
