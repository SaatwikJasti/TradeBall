package com.tradeball.controller;

import com.tradeball.dto.FantasyValueResponse;
import com.tradeball.dto.PageResponse;
import com.tradeball.dto.PlayerResponse;
import com.tradeball.dto.PlayerStatsResponse;
import com.tradeball.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
@Tag(name = "Players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    @Operation(summary = "List players (paginated)")
    public PageResponse<PlayerResponse> list(
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return playerService.list(pageable);
    }

    @GetMapping("/search")
    @Operation(summary = "Search players by name, team, or position")
    public PageResponse<PlayerResponse> search(
            @RequestParam("q") String q,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return playerService.search(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get player by id")
    public PlayerResponse get(@PathVariable Long id) {
        return playerService.getById(id);
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get player season stats")
    public PlayerStatsResponse stats(@PathVariable Long id,
                                     @RequestParam(value = "season", required = false) Integer season) {
        return playerService.getStats(id, season);
    }

    @GetMapping("/{id}/fantasy-value")
    @Operation(summary = "Get fantasy valuation for a player")
    public FantasyValueResponse fantasyValue(@PathVariable Long id) {
        return playerService.getFantasyValue(id);
    }
}
