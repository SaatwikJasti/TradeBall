package com.tradeball.repository;

import com.tradeball.entity.DataSyncJobEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSyncJobRepository extends JpaRepository<DataSyncJobEntity, Long> {
    List<DataSyncJobEntity> findTop20ByOrderByStartedAtDesc();
}
