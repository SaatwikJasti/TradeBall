package com.tradeball.dto;

import com.tradeball.domain.SyncJobStatus;
import com.tradeball.domain.SyncJobType;
import java.time.Instant;

public record SyncJobResponse(
        Long id,
        SyncJobType type,
        SyncJobStatus status,
        Instant startedAt,
        Instant completedAt,
        Integer recordsProcessed,
        Integer recordsFailed,
        String errorMessage
) {
}
