package com.tradeball.repository;

import com.tradeball.entity.PlayerStatsEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerStatsRepository extends JpaRepository<PlayerStatsEntity, Long> {
    Optional<PlayerStatsEntity> findByPlayerIdAndSeason(Long playerId, Integer season);

    List<PlayerStatsEntity> findByPlayerId(Long playerId);

    @Query("SELECT s FROM PlayerStatsEntity s JOIN FETCH s.player p WHERE s.season = :season AND p.active = true")
    List<PlayerStatsEntity> findActiveBySeason(@Param("season") Integer season);
}
