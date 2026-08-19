package com.tradeball.mapper;

import com.tradeball.dto.PlayerResponse;
import com.tradeball.dto.PlayerStatsResponse;
import com.tradeball.entity.PlayerEntity;
import com.tradeball.entity.PlayerStatsEntity;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    public PlayerResponse toResponse(PlayerEntity entity) {
        return new PlayerResponse(
                entity.getId(),
                entity.getExternalId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.fullName(),
                entity.getPosition(),
                entity.getTeam(),
                entity.getAge(),
                entity.isActive()
        );
    }

    public PlayerStatsResponse toStatsResponse(PlayerStatsEntity stats) {
        return new PlayerStatsResponse(
                stats.getPlayer().getId(),
                stats.getSeason(),
                stats.getGamesPlayed(),
                stats.getPoints(),
                stats.getRebounds(),
                stats.getAssists(),
                stats.getSteals(),
                stats.getBlocks(),
                stats.getThreePointers(),
                stats.getFieldGoalPercentage(),
                stats.getFreeThrowPercentage(),
                stats.getTurnovers()
        );
    }
}
