package com.tradeball.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record TradeEvaluateRequest(
        @NotEmpty List<@NotNull @Positive Long> incomingPlayerIds,
        @NotEmpty List<@NotNull @Positive Long> outgoingPlayerIds,
        @Positive Long rosterId
) {
}
