package com.tradeball.service;

import com.tradeball.config.NbaApiProperties;
import com.tradeball.domain.FantasyScore;
import com.tradeball.domain.PopulationStats;
import com.tradeball.dto.FantasyValueResponse;
import com.tradeball.dto.PageResponse;
import com.tradeball.dto.PlayerResponse;
import com.tradeball.dto.PlayerStatsResponse;
import com.tradeball.entity.PlayerEntity;
import com.tradeball.entity.PlayerStatsEntity;
import com.tradeball.exception.ResourceNotFoundException;
import com.tradeball.mapper.PlayerMapper;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.PlayerStatsRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final PlayerMapper playerMapper;
    private final FantasyEvaluationService fantasyEvaluationService;
    private final PopulationStatsService populationStatsService;
    private final StatsMapperHelper statsMapperHelper;
    private final NbaApiProperties nbaApiProperties;

    public PlayerService(PlayerRepository playerRepository,
                         PlayerStatsRepository playerStatsRepository,
                         PlayerMapper playerMapper,
                         FantasyEvaluationService fantasyEvaluationService,
                         PopulationStatsService populationStatsService,
                         StatsMapperHelper statsMapperHelper,
                         NbaApiProperties nbaApiProperties) {
        this.playerRepository = playerRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.playerMapper = playerMapper;
        this.fantasyEvaluationService = fantasyEvaluationService;
        this.populationStatsService = populationStatsService;
        this.statsMapperHelper = statsMapperHelper;
        this.nbaApiProperties = nbaApiProperties;
    }

    @Transactional(readOnly = true)
    public PageResponse<PlayerResponse> list(Pageable pageable) {
        Page<PlayerResponse> page = playerRepository.findByActiveTrue(pageable).map(playerMapper::toResponse);
        return PageResponse.from(page);
    }

    @Cacheable(cacheNames = "players", key = "#id")
    @Transactional(readOnly = true)
    public PlayerResponse getById(Long id) {
        return playerMapper.toResponse(requirePlayer(id));
    }

    @Cacheable(cacheNames = "playerSearch", key = "#q + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PageResponse<PlayerResponse> search(String q, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        Page<PlayerResponse> page = playerRepository.search(query, pageable).map(playerMapper::toResponse);
        return PageResponse.from(page);
    }

    @Cacheable(cacheNames = "playerStats", key = "#id + '-' + #season")
    @Transactional(readOnly = true)
    public PlayerStatsResponse getStats(Long id, Integer season) {
        requirePlayer(id);
        int effectiveSeason = season == null ? nbaApiProperties.season() : season;
        PlayerStatsEntity stats = playerStatsRepository.findByPlayerIdAndSeason(id, effectiveSeason)
                .orElseThrow(() -> new ResourceNotFoundException("Stats not found for player " + id));
        return playerMapper.toStatsResponse(stats);
    }

    @Cacheable(cacheNames = "fantasyValues", key = "#id")
    @Transactional(readOnly = true)
    public FantasyValueResponse getFantasyValue(Long id) {
        PlayerEntity player = requirePlayer(id);
        PlayerStatsEntity stats = playerStatsRepository.findByPlayerIdAndSeason(id, nbaApiProperties.season())
                .orElseThrow(() -> new ResourceNotFoundException("Stats not found for player " + id));
        PopulationStats population = populationStatsService.currentSeasonPopulation();
        List<Double> allScores = populationStatsService.allRawFantasyScores();
        FantasyScore score = fantasyEvaluationService.evaluateWithNormalization(
                statsMapperHelper.toCategoryStatistics(stats), population, allScores);

        Map<String, Double> z = new LinkedHashMap<>();
        Map<String, Double> contrib = new LinkedHashMap<>();
        score.zScores().asMap().forEach((k, v) -> z.put(k.name(), v));
        score.contributions().asMap().forEach((k, v) -> contrib.put(k.name(), v));

        return new FantasyValueResponse(
                player.getId(),
                player.fullName(),
                stats.getSeason(),
                score.rawScore(),
                score.normalizedScore(),
                z,
                contrib,
                fantasyEvaluationService.modelVersion()
        );
    }

    public PlayerEntity requirePlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }
}
