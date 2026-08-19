package com.tradeball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tradeball.config.FantasyScoringProperties;
import com.tradeball.domain.CategoryStatistics;
import com.tradeball.domain.FantasyCategory;
import com.tradeball.domain.FantasyScore;
import com.tradeball.domain.PopulationStats;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FantasyEvaluationServiceTest {

    private FantasyEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new FantasyEvaluationService(new FantasyScoringProperties());
    }

    @Test
    void calculatesWeightedZScoreSum() {
        CategoryStatistics average = stats(20, 5, 5, 1, 1, 2, 45, 80, 2);
        CategoryStatistics elite = stats(30, 10, 10, 2, 2, 3, 55, 90, 1);
        PopulationStats population = service.computePopulationStats(List.of(average, elite));

        FantasyScore avgScore = service.evaluate(average, population);
        FantasyScore eliteScore = service.evaluate(elite, population);

        assertTrue(eliteScore.rawScore() > avgScore.rawScore());
    }

    @Test
    void handlesZeroStandardDeviationSafely() {
        CategoryStatistics a = stats(20, 5, 5, 1, 1, 2, 45, 80, 2);
        CategoryStatistics b = stats(20, 5, 5, 1, 1, 2, 45, 80, 2);
        PopulationStats population = service.computePopulationStats(List.of(a, b));

        for (FantasyCategory category : FantasyCategory.values()) {
            assertEquals(1.0, population.stdDev(category), 0.0001);
        }

        FantasyScore score = service.evaluate(a, population);
        assertEquals(0.0, score.rawScore(), 0.0001);
    }

    @Test
    void normalizeMapsMinMaxToZeroAndHundred() {
        assertEquals(0, service.normalize(0, List.of(0.0, 10.0)));
        assertEquals(100, service.normalize(10, List.of(0.0, 10.0)));
        assertEquals(50, service.normalize(5, List.of(5.0, 5.0)));
    }

    @Test
    void turnoverWeightIsNegative() {
        assertEquals(-1.0, service.weights().get(FantasyCategory.TO));
    }

    private CategoryStatistics stats(double pts, double reb, double ast, double stl, double blk,
                                     double three, double fg, double ft, double to) {
        Map<FantasyCategory, Double> map = new EnumMap<>(FantasyCategory.class);
        map.put(FantasyCategory.PTS, pts);
        map.put(FantasyCategory.REB, reb);
        map.put(FantasyCategory.AST, ast);
        map.put(FantasyCategory.STL, stl);
        map.put(FantasyCategory.BLK, blk);
        map.put(FantasyCategory.THREE_PM, three);
        map.put(FantasyCategory.FG_PCT, fg);
        map.put(FantasyCategory.FT_PCT, ft);
        map.put(FantasyCategory.TO, to);
        return new CategoryStatistics(map);
    }
}
