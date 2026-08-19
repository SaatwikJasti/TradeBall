package com.tradeball.dto;

import java.time.Instant;
import java.util.List;

public record RosterResponse(
        Long id,
        String name,
        List<PlayerResponse> players,
        Instant createdAt,
        Instant updatedAt
) {
}
