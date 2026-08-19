package com.tradeball.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TradeEvaluateRequest(
        @NotEmpty List<Long> incomingPlayerIds,
        @NotEmpty List<Long> outgoingPlayerIds,
        Long rosterId
) {
}
