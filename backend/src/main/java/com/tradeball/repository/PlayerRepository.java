package com.tradeball.repository;

import com.tradeball.entity.PlayerEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
    Optional<PlayerEntity> findByExternalId(String externalId);

    Optional<PlayerEntity> findFirstByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

    Page<PlayerEntity> findByActiveTrue(Pageable pageable);

    @Query("""
            SELECT p FROM PlayerEntity p
            WHERE p.active = true
              AND (
                   LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.team) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.position) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<PlayerEntity> search(@Param("q") String q, Pageable pageable);

    List<PlayerEntity> findByActiveTrue();
}
