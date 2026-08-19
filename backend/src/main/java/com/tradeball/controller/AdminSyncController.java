package com.tradeball.controller;

import com.tradeball.dto.SyncJobResponse;
import com.tradeball.service.NbaDataSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sync")
@Tag(name = "Admin Sync")
public class AdminSyncController {

    private final NbaDataSyncService nbaDataSyncService;

    public AdminSyncController(NbaDataSyncService nbaDataSyncService) {
        this.nbaDataSyncService = nbaDataSyncService;
    }

    @GetMapping("/status")
    @Operation(summary = "Recent sync job status")
    public List<SyncJobResponse> status() {
        return nbaDataSyncService.status();
    }

    @PostMapping("/players")
    @Operation(summary = "Synchronize players from external NBA source")
    public SyncJobResponse syncPlayers() {
        return nbaDataSyncService.syncPlayers();
    }

    @PostMapping("/stats")
    @Operation(summary = "Synchronize player statistics from external NBA source")
    public SyncJobResponse syncStats() {
        return nbaDataSyncService.syncStats();
    }
}
