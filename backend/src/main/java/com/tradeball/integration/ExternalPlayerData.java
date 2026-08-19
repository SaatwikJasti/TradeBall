package com.tradeball.integration;

/**
 * Normalized external player payload independent of any third-party API shape.
 */
public record ExternalPlayerData(
        String externalId,
        String firstName,
        String lastName,
        String position,
        String team,
        Integer age,
        boolean active
) {
}
