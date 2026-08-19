package com.tradeball.repository;

import com.tradeball.entity.TradeEvaluationEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeEvaluationRepository extends JpaRepository<TradeEvaluationEntity, Long> {
    Page<TradeEvaluationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<TradeEvaluationEntity> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT t FROM TradeEvaluationEntity t
            WHERE t.id = :id AND t.user.id = :userId
            """)
    Optional<TradeEvaluationEntity> findDetailedByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
