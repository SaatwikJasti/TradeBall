package com.tradeball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tradeball.config.NbaApiProperties;
import com.tradeball.domain.SyncJobStatus;
import com.tradeball.domain.SyncJobType;
import com.tradeball.dto.SyncJobResponse;
import com.tradeball.entity.DataSyncJobEntity;
import com.tradeball.entity.PlayerEntity;
import com.tradeball.integration.ExternalPlayerData;
import com.tradeball.integration.NbaStatsClient;
import com.tradeball.repository.DataSyncJobRepository;
import com.tradeball.repository.PlayerRepository;
import com.tradeball.repository.PlayerStatsRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class NbaDataSyncServiceTest {

    @Mock NbaStatsClient nbaStatsClient;
    @Mock PlayerRepository playerRepository;
    @Mock PlayerStatsRepository playerStatsRepository;
    @Mock DataSyncJobRepository dataSyncJobRepository;
    @Mock TransactionTemplate transactionTemplate;

    NbaDataSyncService service;
    AtomicLong jobIds;

    @BeforeEach
    void setUp() {
        jobIds = new AtomicLong(1);
        NbaApiProperties properties = new NbaApiProperties(
                "https://api.server.nbaapi.com", 2026, 5000, 30000, 1, false);
        service = new NbaDataSyncService(
                nbaStatsClient,
                playerRepository,
                playerStatsRepository,
                dataSyncJobRepository,
                properties,
                new ConcurrentMapCacheManager("players", "playerSearch", "playerStats", "fantasyValues"),
                transactionTemplate
        );

        when(transactionTemplate.execute(any())).thenAnswer((Answer<Object>) invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void syncPlayersUpsertsFetchedPlayersAndSucceeds() {
        when(nbaStatsClient.fetchPlayers(2026)).thenReturn(List.of(
                new ExternalPlayerData("curryst01", "Stephen", "Curry", "PG", "GSW", 37, true),
                new ExternalPlayerData("jokicni01", "Nikola", "Jokic", "C", "DEN", 30, true)
        ));
        when(playerRepository.findByExternalId(any())).thenReturn(Optional.empty());
        when(playerRepository.findFirstByFirstNameIgnoreCaseAndLastNameIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());
        when(playerRepository.save(any(PlayerEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        stubJobPersistence();

        SyncJobResponse response = service.syncPlayers();

        assertEquals(SyncJobStatus.SUCCEEDED, response.status());
        assertEquals(2, response.recordsProcessed());
        assertEquals(0, response.recordsFailed());
        verify(nbaStatsClient, times(1)).fetchPlayers(2026);
        verify(playerRepository, times(2)).save(any(PlayerEntity.class));
    }

    @Test
    void syncPlayersIsIdempotentForExistingExternalIds() {
        PlayerEntity existing = new PlayerEntity();
        existing.setId(9L);
        existing.setExternalId("curryst01");
        existing.setFirstName("Stephen");
        existing.setLastName("Curry");

        when(nbaStatsClient.fetchPlayers(2026)).thenReturn(List.of(
                new ExternalPlayerData("curryst01", "Stephen", "Curry", "PG", "GSW", 38, true)
        ));
        when(playerRepository.findByExternalId("curryst01")).thenReturn(Optional.of(existing));
        when(playerRepository.save(any(PlayerEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        stubJobPersistence();

        SyncJobResponse response = service.syncPlayers();

        assertEquals(SyncJobStatus.SUCCEEDED, response.status());
        ArgumentCaptor<PlayerEntity> captor = ArgumentCaptor.forClass(PlayerEntity.class);
        verify(playerRepository).save(captor.capture());
        assertEquals(9L, captor.getValue().getId());
        assertEquals(38, captor.getValue().getAge());
        verify(playerRepository, never())
                .findFirstByFirstNameIgnoreCaseAndLastNameIgnoreCase(any(), any());
    }

    @Test
    void syncPlayersRecordsFailureWhenProviderThrows() {
        when(nbaStatsClient.fetchPlayers(anyInt()))
                .thenThrow(new RuntimeException("provider down"));
        stubJobPersistence();

        SyncJobResponse response = service.syncPlayers();

        assertEquals(SyncJobStatus.FAILED, response.status());
        assertEquals(SyncJobType.PLAYERS, response.type());
        assertTrue(response.errorMessage().contains("provider down"));
        verify(playerRepository, never()).save(any());
    }

    private void stubJobPersistence() {
        when(dataSyncJobRepository.save(any(DataSyncJobEntity.class))).thenAnswer(inv -> {
            DataSyncJobEntity job = inv.getArgument(0);
            if (job.getId() == null) {
                job.setId(jobIds.getAndIncrement());
            }
            return job;
        });
        when(dataSyncJobRepository.findById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            DataSyncJobEntity job = new DataSyncJobEntity();
            job.setId(id);
            job.setType(SyncJobType.PLAYERS);
            job.setStatus(SyncJobStatus.RUNNING);
            return Optional.of(job);
        });
    }
}
