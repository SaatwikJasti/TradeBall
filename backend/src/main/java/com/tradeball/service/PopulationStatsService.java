package com.tradeball.service;

import com.tradeball.config.NbaApiProperties;
import com.tradeball.domain.CategoryStatistics;
import com.tradeball.domain.PopulationStats;
import com.tradeball.entity.PlayerStatsEntity;
import com.tradeball.repository.PlayerStatsRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PopulationStatsService {

    private final PlayerStatsRepository playerStatsRepository;
    private final FantasyEvaluationService fantasyEvaluationService;
    private final StatsMapperHelper statsMapperHelper;
    private final NbaApiProperties nbaApiProperties;

    public PopulationStatsService(PlayerStatsRepository playerStatsRepository,
                                  FantasyEvaluationService fantasyEvaluationService,
                                  StatsMapperHelper statsMapperHelper,
                                  NbaApiProperties nbaApiProperties) {
        this.playerStatsRepository = playerStatsRepository;
        this.fantasyEvaluationService = fantasyEvaluationService;
        this.statsMapperHelper = statsMapperHelper;
        this.nbaApiProperties = nbaApiProperties;
    }

    @Transactional(readOnly = true)
    public PopulationStats currentSeasonPopulation() {
        List<PlayerStatsEntity> stats = playerStatsRepository.findActiveBySeason(nbaApiProperties.season());
        List<CategoryStatistics> population = new ArrayList<>(stats.size());
        for (PlayerStatsEntity entity : stats) {
            population.add(statsMapperHelper.toCategoryStatistics(entity));
        }
        if (population.isEmpty()) {
            return fantasyEvaluationService.computePopulationStats(List.of(emptyStats()));
        }
        return fantasyEvaluationService.computePopulationStats(population);
    }

    @Transactional(readOnly = true)
    public List<Double> allRawFantasyScores() {
        PopulationStats population = currentSeasonPopulation();
        List<PlayerStatsEntity> stats = playerStatsRepository.findActiveBySeason(nbaApiProperties.season());
        List<Double> scores = new ArrayList<>(stats.size());
        for (PlayerStatsEntity entity : stats) {
            scores.add(fantasyEvaluationService.evaluate(statsMapperHelper.toCategoryStatistics(entity), population).rawScore());
        }
        return scores;
    }

    private CategoryStatistics emptyStats() {
        return new CategoryStatistics(java.util.Map.of());
    }
}
