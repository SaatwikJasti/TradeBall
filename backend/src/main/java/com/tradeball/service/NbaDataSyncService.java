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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Synchronizes NBA players/stats into PostgreSQL.
 * External HTTP calls deliberately run outside DB transactions so Supabase/Render
 * connections are not held open during multi-page provider fetches.
 */
@Service
public class NbaDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(NbaDataSyncService.class);

    private final NbaStatsClient nbaStatsClient;
    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final DataSyncJobRepository dataSyncJobRepository;
    private final NbaApiProperties nbaApiProperties;
    private final CacheManager cacheManager;
    private final TransactionTemplate transactionTemplate;

    public NbaDataSyncService(NbaStatsClient nbaStatsClient,
                              PlayerRepository playerRepository,
                              PlayerStatsRepository playerStatsRepository,
                              DataSyncJobRepository dataSyncJobRepository,
                              NbaApiProperties nbaApiProperties,
                              CacheManager cacheManager,
                              TransactionTemplate transactionTemplate) {
        this.nbaStatsClient = nbaStatsClient;
        this.playerRepository = playerRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.dataSyncJobRepository = dataSyncJobRepository;
        this.nbaApiProperties = nbaApiProperties;
        this.cacheManager = cacheManager;
        this.transactionTemplate = transactionTemplate;
    }

    public SyncJobResponse syncPlayers() {
        int season = nbaApiProperties.season();
        Long jobId = startJob(SyncJobType.PLAYERS);
        int processed = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        try {
            log.info("Starting player sync season={} jobId={}", season, jobId);
            List<ExternalPlayerData> external = nbaStatsClient.fetchPlayers(season);
            log.info("Fetched {} external players for season {}", external.size(), season);

            SyncCounts counts = transactionTemplate.execute(status -> upsertPlayers(external));
            processed = counts == null ? 0 : counts.processed();
            failed = counts == null ? 0 : counts.failed();
            errors.addAll(counts == null ? List.of() : counts.errors());

            SyncJobResponse response = completeJob(jobId, processed, failed, errors);
            evictPlayerCaches();
            return response;
        } catch (Exception ex) {
            log.error("Player sync failed jobId={} season={}", jobId, season, ex);
            return failJob(jobId, processed, failed, safeMessage(ex));
        }
    }

    public SyncJobResponse syncStats() {
        int season = nbaApiProperties.season();
        Long jobId = startJob(SyncJobType.STATS);
        int processed = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        try {
            log.info("Starting stats sync season={} jobId={}", season, jobId);
            List<ExternalPlayerData> players = nbaStatsClient.fetchPlayers(season);
            List<ExternalPlayerStatsData> stats = nbaStatsClient.fetchPlayerStats(season);
            log.info("Fetched {} players and {} stat rows for season {}", players.size(), stats.size(), season);

            transactionTemplate.executeWithoutResult(status -> {
                for (ExternalPlayerData player : players) {
                    upsertPlayer(player);
                }
            });

            SyncCounts counts = transactionTemplate.execute(status -> upsertStats(stats));
            processed = counts == null ? 0 : counts.processed();
            failed = counts == null ? 0 : counts.failed();
            errors.addAll(counts == null ? List.of() : counts.errors());

            SyncJobResponse response = completeJob(jobId, processed, failed, errors);
            evictPlayerCaches();
            return response;
        } catch (Exception ex) {
            log.error("Stats sync failed jobId={} season={}", jobId, season, ex);
            return failJob(jobId, processed, failed, safeMessage(ex));
        }
    }

    @Transactional(readOnly = true)
    public List<SyncJobResponse> status() {
        return dataSyncJobRepository.findTop20ByOrderByStartedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private SyncCounts upsertPlayers(List<ExternalPlayerData> external) {
        int processed = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
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
        return new SyncCounts(processed, failed, errors);
    }

    private SyncCounts upsertStats(List<ExternalPlayerStatsData> external) {
        int processed = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (ExternalPlayerStatsData data : external) {
            try {
                upsertStatRow(data);
                processed++;
            } catch (Exception ex) {
                failed++;
                errors.add(data.externalId() + ": " + ex.getMessage());
                log.warn("Stats upsert failed externalId={}", data.externalId(), ex);
            }
        }
        return new SyncCounts(processed, failed, errors);
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

    private void upsertStatRow(ExternalPlayerStatsData data) {
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

    private Long startJob(SyncJobType type) {
        return transactionTemplate.execute(status -> {
            DataSyncJobEntity job = new DataSyncJobEntity();
            job.setType(type);
            job.setStatus(SyncJobStatus.RUNNING);
            job.setStartedAt(Instant.now());
            return dataSyncJobRepository.save(job).getId();
        });
    }

    private SyncJobResponse completeJob(Long jobId, int processed, int failed, List<String> errors) {
        return transactionTemplate.execute(status -> {
            DataSyncJobEntity job = dataSyncJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalStateException("Missing sync job " + jobId));
            job.setRecordsProcessed(processed);
            job.setRecordsFailed(failed);
            job.setCompletedAt(Instant.now());
            if (processed == 0 && failed == 0) {
                job.setStatus(SyncJobStatus.FAILED);
                job.setErrorMessage("No player records returned from NBA provider for season "
                        + nbaApiProperties.season());
            } else if (failed == 0) {
                job.setStatus(SyncJobStatus.SUCCEEDED);
                job.setErrorMessage(null);
            } else if (processed > 0) {
                job.setStatus(SyncJobStatus.PARTIAL);
                job.setErrorMessage(String.join("; ", errors.stream().limit(5).toList()));
            } else {
                job.setStatus(SyncJobStatus.FAILED);
                job.setErrorMessage(String.join("; ", errors.stream().limit(5).toList()));
            }
            DataSyncJobEntity saved = dataSyncJobRepository.save(job);
            log.info("Sync job completed id={} type={} status={} processed={} failed={}",
                    saved.getId(), saved.getType(), saved.getStatus(), processed, failed);
            return toResponse(saved);
        });
    }

    private SyncJobResponse failJob(Long jobId, int processed, int failed, String message) {
        return transactionTemplate.execute(status -> {
            DataSyncJobEntity job = dataSyncJobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalStateException("Missing sync job " + jobId));
            job.setRecordsProcessed(processed);
            job.setRecordsFailed(failed);
            job.setCompletedAt(Instant.now());
            job.setStatus(SyncJobStatus.FAILED);
            job.setErrorMessage(message);
            return toResponse(dataSyncJobRepository.save(job));
        });
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

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private record SyncCounts(int processed, int failed, List<String> errors) {
    }
}
