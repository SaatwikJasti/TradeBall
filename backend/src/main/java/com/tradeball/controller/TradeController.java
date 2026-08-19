package com.tradeball.controller;

import com.tradeball.dto.PageResponse;
import com.tradeball.dto.TradeEvaluateRequest;
import com.tradeball.dto.TradeEvaluationResponse;
import com.tradeball.service.TradeEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trades")
@Tag(name = "Trades")
public class TradeController {

    private final TradeEvaluationService tradeEvaluationService;

    public TradeController(TradeEvaluationService tradeEvaluationService) {
        this.tradeEvaluationService = tradeEvaluationService;
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate a trade")
    public TradeEvaluationResponse evaluate(@Valid @RequestBody TradeEvaluateRequest request) {
        return tradeEvaluationService.evaluate(request);
    }

    @GetMapping
    @Operation(summary = "Trade evaluation history")
    public PageResponse<TradeEvaluationResponse> history(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return tradeEvaluationService.history(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a persisted trade evaluation")
    public TradeEvaluationResponse get(@PathVariable Long id) {
        return tradeEvaluationService.get(id);
    }
}
