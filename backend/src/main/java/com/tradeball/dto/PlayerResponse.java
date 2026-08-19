package com.tradeball.dto;

public record PlayerResponse(
        Long id,
        String externalId,
        String firstName,
        String lastName,
        String fullName,
        String position,
        String team,
        Integer age,
        boolean active
) {
}
