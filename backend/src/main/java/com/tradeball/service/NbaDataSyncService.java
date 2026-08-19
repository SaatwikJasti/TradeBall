package com.tradeball.service;

import com.tradeball.config.NbaApiProperties;
import com.tradeball.domain.SyncJobStatus;
import com.tradeball.domain.SyncJobType;
import com.tradeball.dto.SyncJobResponse;
import com.tradeball.entity.DataSyncJobEntity;
import com.tradeball.entity.PlayerEntity;
import com.tradeball.entity.PlayerStatsEntity;
import com.tradeball.integration.ExternalPlayerData;
import com.tradeball.integration.ExternalPlayerStatsData;
import com.tradeball.integration.NbaStatsClient;
import com.tradeball.repository.DataSyncJobRepository;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.PlayerStatsRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NbaDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(NbaDataSyncService.class);

    private final NbaStatsClient nbaStatsClient;
    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final DataSyncJobRepository dataSyncJobRepository;
    private final NbaApiProperties nbaApiProperties;
    private final CacheManager cacheManager;

    public NbaDataSyncService(NbaStatsClient nbaStatsClient,
                              PlayerRepository playerRepository,
                              PlayerStatsRepository playerStatsRepository,
                              DataSyncJobRepository dataSyncJobRepository,
                              NbaApiProperties nbaApiProperties,
                              CacheManager cacheManager) {
        this.nbaStatsClient = nbaStatsClient;
        this.playerRepository = playerRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.dataSyncJobRepository = dataSyncJobRepository;
        this.nbaApiProperties = nbaApiProperties;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public SyncJobResponse syncPlayers() {
        DataSyncJobEntity job = startJob(SyncJobType.PLAYERS);
        int processed = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        try {
            List<ExternalPlayerData> external = nbaStatsClient.fetchPlayers(nbaApiProperties.season());
            for (ExternalPlayerData data : external) {
                try {
                    upsertPlayer(data);
                    processed++;
                } catch (Exception ex) {
                    failed++;
                    errors.add(data.externalId() + ": " + ex.getMessage());
                    log.warn("Player upsert failed externalId={}", data.externalId(), ex);
                }
            }
            completeJob(job, processed, failed, errors);
            evictPlayerCaches();
        } catch (Exception ex) {
            failJob(job, processed, failed, ex.getMessage());
            log.error("Player sync failed", ex);
        }
        return toResponse(job);
    }

    @Transactional
    public SyncJobResponse syncStats() {
        DataSyncJobEntity job = startJob(SyncJobType.STATS);
        int processed = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        try {
            // Ensure players exist first (idempotent)
            for (ExternalPlayerData player : nbaStatsClient.fetchPlayers(nbaApiProperties.season())) {
                upsertPlayer(player);
            }
            List<ExternalPlayerStatsData> external = nbaStatsClient.fetchPlayerStats(nbaApiProperties.season());
            for (ExternalPlayerStatsData data : external) {
                try {
                    upsertStats(data);
                    processed++;
                } catch (Exception ex) {
                    failed++;
                    errors.add(data.externalId() + ": " + ex.getMessage());
                    log.warn("Stats upsert failed externalId={}", data.externalId(), ex);
                }
            }
            completeJob(job, processed, failed, errors);
            evictPlayerCaches();
        } catch (Exception ex) {
            failJob(job, processed, failed, ex.getMessage());
            log.error("Stats sync failed", ex);
        }
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<SyncJobResponse> status() {
        return dataSyncJobRepository.findTop20ByOrderByStartedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private void upsertPlayer(ExternalPlayerData data) {
        if (data.externalId() == null || data.externalId().isBlank()) {
            throw new IllegalArgumentException("externalId required");
        }
        PlayerEntity player = playerRepository.findByExternalId(data.externalId())
                .or(() -> playerRepository.findFirstByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                        nullToEmpty(data.firstName()), nullToEmpty(data.lastName())))
                .orElseGet(PlayerEntity::new);
        player.setExternalId(data.externalId());
        player.setFirstName(nullToEmpty(data.firstName()));
        player.setLastName(nullToEmpty(data.lastName()));
        player.setPosition(data.position());
        player.setTeam(data.team());
        player.setAge(data.age());
        player.setActive(data.active());
        playerRepository.save(player);
    }

    private void upsertStats(ExternalPlayerStatsData data) {
        PlayerEntity player = playerRepository.findByExternalId(data.externalId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown player " + data.externalId()));
        PlayerStatsEntity stats = playerStatsRepository
                .findByPlayerIdAndSeason(player.getId(), data.season())
                .orElseGet(PlayerStatsEntity::new);
        stats.setPlayer(player);
        stats.setSeason(data.season());
        stats.setGamesPlayed(data.gamesPlayed() == null ? 0 : data.gamesPlayed());
        stats.setPoints(nz(data.points()));
        stats.setRebounds(nz(data.rebounds()));
        stats.setAssists(nz(data.assists()));
        stats.setSteals(nz(data.steals()));
        stats.setBlocks(nz(data.blocks()));
        stats.setThreePointers(nz(data.threePointers()));
        stats.setFieldGoalPercentage(nz(data.fieldGoalPercentage()));
        stats.setFreeThrowPercentage(nz(data.freeThrowPercentage()));
        stats.setTurnovers(nz(data.turnovers()));
        playerStatsRepository.save(stats);
    }

    private DataSyncJobEntity startJob(SyncJobType type) {
        DataSyncJobEntity job = new DataSyncJobEntity();
        job.setType(type);
        job.setStatus(SyncJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        return dataSyncJobRepository.save(job);
    }

    private void completeJob(DataSyncJobEntity job, int processed, int failed, List<String> errors) {
        job.setRecordsProcessed(processed);
        job.setRecordsFailed(failed);
        job.setCompletedAt(Instant.now());
        if (failed == 0) {
            job.setStatus(SyncJobStatus.SUCCEEDED);
        } else if (processed > 0) {
            job.setStatus(SyncJobStatus.PARTIAL);
            job.setErrorMessage(String.join("; ", errors.stream().limit(5).toList()));
        } else {
            job.setStatus(SyncJobStatus.FAILED);
            job.setErrorMessage(String.join("; ", errors.stream().limit(5).toList()));
        }
        dataSyncJobRepository.save(job);
        log.info("Sync job completed id={} type={} status={} processed={} failed={}",
                job.getId(), job.getType(), job.getStatus(), processed, failed);
    }

    private void failJob(DataSyncJobEntity job, int processed, int failed, String message) {
        job.setRecordsProcessed(processed);
        job.setRecordsFailed(failed);
        job.setCompletedAt(Instant.now());
        job.setStatus(SyncJobStatus.FAILED);
        job.setErrorMessage(message);
        dataSyncJobRepository.save(job);
    }

    private void evictPlayerCaches() {
        for (String name : List.of("players", "playerSearch", "playerStats", "fantasyValues")) {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private SyncJobResponse toResponse(DataSyncJobEntity job) {
        return new SyncJobResponse(
                job.getId(),
                job.getType(),
                job.getStatus(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getRecordsProcessed(),
                job.getRecordsFailed(),
                job.getErrorMessage()
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static double nz(Double value) {
        return value == null ? 0.0 : value;
    }
}
