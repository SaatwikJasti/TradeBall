package com.tradeball.repository;

import com.tradeball.entity.RosterEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RosterRepository extends JpaRepository<RosterEntity, Long> {
    List<RosterEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT r FROM RosterEntity r LEFT JOIN FETCH r.players WHERE r.id = :id")
    Optional<RosterEntity> findByIdWithPlayers(@Param("id") Long id);

    Optional<RosterEntity> findByIdAndUserId(Long id, Long userId);
}
