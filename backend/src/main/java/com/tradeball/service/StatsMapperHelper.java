package com.tradeball.service;

import com.tradeball.domain.CategoryStatistics;
import com.tradeball.domain.FantasyCategory;
import com.tradeball.entity.PlayerStatsEntity;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StatsMapperHelper {

    public CategoryStatistics toCategoryStatistics(PlayerStatsEntity stats) {
        Map<FantasyCategory, Double> values = new EnumMap<>(FantasyCategory.class);
        values.put(FantasyCategory.PTS, nullSafe(stats.getPoints()));
        values.put(FantasyCategory.REB, nullSafe(stats.getRebounds()));
        values.put(FantasyCategory.AST, nullSafe(stats.getAssists()));
        values.put(FantasyCategory.STL, nullSafe(stats.getSteals()));
        values.put(FantasyCategory.BLK, nullSafe(stats.getBlocks()));
        values.put(FantasyCategory.THREE_PM, nullSafe(stats.getThreePointers()));
        values.put(FantasyCategory.FG_PCT, nullSafe(stats.getFieldGoalPercentage()));
        values.put(FantasyCategory.FT_PCT, nullSafe(stats.getFreeThrowPercentage()));
        values.put(FantasyCategory.TO, nullSafe(stats.getTurnovers()));
        return new CategoryStatistics(values);
    }

    private double nullSafe(Double value) {
        return value == null ? 0.0 : value;
    }
}
