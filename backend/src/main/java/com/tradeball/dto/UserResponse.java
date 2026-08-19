package com.tradeball.dto;

import com.tradeball.domain.Role;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        Instant createdAt
) {
}
