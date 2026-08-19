package com.tradeball.controller;

import com.tradeball.dto.RosterRequest;
import com.tradeball.dto.RosterResponse;
import com.tradeball.service.RosterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rosters")
@Tag(name = "Rosters")
public class RosterController {

    private final RosterService rosterService;

    public RosterController(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create roster")
    public RosterResponse create(@Valid @RequestBody RosterRequest request) {
        return rosterService.create(request);
    }

    @GetMapping
    @Operation(summary = "List my rosters")
    public List<RosterResponse> list() {
        return rosterService.listMine();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get roster")
    public RosterResponse get(@PathVariable Long id) {
        return rosterService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update roster")
    public RosterResponse update(@PathVariable Long id, @Valid @RequestBody RosterRequest request) {
        return rosterService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete roster")
    public void delete(@PathVariable Long id) {
        rosterService.delete(id);
    }

    @PostMapping("/{id}/players/{playerId}")
    @Operation(summary = "Add player to roster")
    public RosterResponse addPlayer(@PathVariable Long id, @PathVariable Long playerId) {
        return rosterService.addPlayer(id, playerId);
    }

    @DeleteMapping("/{id}/players/{playerId}")
    @Operation(summary = "Remove player from roster")
    public RosterResponse removePlayer(@PathVariable Long id, @PathVariable Long playerId) {
        return rosterService.removePlayer(id, playerId);
    }
}
