package com.tradeball.service;

import com.tradeball.config.FantasyScoringProperties;
import com.tradeball.domain.CategoryStatistics;
import com.tradeball.domain.CategoryZScores;
import com.tradeball.domain.FantasyCategory;
import com.tradeball.domain.FantasyScore;
import com.tradeball.domain.PopulationStats;
import com.tradeball.domain.WeightedContributions;
import com.tradeball.util.StatMath;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Z-score fantasy valuation engine matching the TradeBall frontend heuristic.
 */
@Service
public class FantasyEvaluationService {

    private final FantasyScoringProperties properties;

    public FantasyEvaluationService(FantasyScoringProperties properties) {
        this.properties = properties;
    }

    public PopulationStats computePopulationStats(List<CategoryStatistics> population) {
        EnumMap<FantasyCategory, Double> means = new EnumMap<>(FantasyCategory.class);
        EnumMap<FantasyCategory, Double> stdDevs = new EnumMap<>(FantasyCategory.class);
        for (FantasyCategory category : FantasyCategory.values()) {
            List<Double> values = new ArrayList<>(population.size());
            for (CategoryStatistics stats : population) {
                values.add(stats.get(category));
            }
            means.put(category, StatMath.mean(values));
            stdDevs.put(category, StatMath.populationStdDev(values));
        }
        return new PopulationStats(means, stdDevs, population.size());
    }

    public CategoryZScores computeZScores(CategoryStatistics stats, PopulationStats population) {
        EnumMap<FantasyCategory, Double> zScores = new EnumMap<>(FantasyCategory.class);
        for (FantasyCategory category : FantasyCategory.values()) {
            double z = (stats.get(category) - population.mean(category)) / population.stdDev(category);
            zScores.put(category, z);
        }
        return new CategoryZScores(zScores);
    }

    public WeightedContributions computeContributions(CategoryZScores zScores) {
        EnumMap<FantasyCategory, Double> contributions = new EnumMap<>(FantasyCategory.class);
        for (FantasyCategory category : FantasyCategory.values()) {
            contributions.put(category, properties.weight(category) * zScores.get(category));
        }
        return new WeightedContributions(contributions);
    }

    public FantasyScore evaluate(CategoryStatistics stats, PopulationStats population) {
        CategoryZScores zScores = computeZScores(stats, population);
        WeightedContributions contributions = computeContributions(zScores);
        return new FantasyScore(contributions.total(), null, zScores, contributions);
    }

    public FantasyScore evaluateWithNormalization(CategoryStatistics stats,
                                                  PopulationStats population,
                                                  List<Double> allRawScores) {
        FantasyScore raw = evaluate(stats, population);
        Integer normalized = normalize(raw.rawScore(), allRawScores);
        return new FantasyScore(raw.rawScore(), normalized, raw.zScores(), raw.contributions());
    }

    public Integer normalize(double rawScore, List<Double> allRawScores) {
        if (allRawScores == null || allRawScores.isEmpty()) {
            return 50;
        }
        double min = allRawScores.stream().mapToDouble(Double::doubleValue).min().orElse(rawScore);
        double max = allRawScores.stream().mapToDouble(Double::doubleValue).max().orElse(rawScore);
        if (max == min) {
            return 50;
        }
        return (int) Math.round(((rawScore - min) / (max - min)) * 100.0);
    }

    public double averageZ(List<CategoryZScores> scores, FantasyCategory category) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        return scores.stream().mapToDouble(s -> s.get(category)).average().orElse(0.0);
    }

    public double sumZ(List<CategoryZScores> scores, FantasyCategory category) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        return scores.stream().mapToDouble(s -> s.get(category)).sum();
    }

    public Map<FantasyCategory, Double> weights() {
        return properties.getWeights();
    }

    public String modelVersion() {
        return properties.getModelVersion();
    }
}
