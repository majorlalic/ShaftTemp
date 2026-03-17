package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.MonitorPartitionBindEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MonitorPartitionBindRepository extends JpaRepository<MonitorPartitionBindEntity, Long> {

    @Query("select b from MonitorPartitionBindEntity b where b.partitionCode = ?1 and b.bindStatus = 1 and (b.deleted is null or b.deleted = 0)")
    Optional<MonitorPartitionBindEntity> findActiveByPartitionCode(String partitionCode);

    @Query("select b from MonitorPartitionBindEntity b where b.dataReference = ?1 and b.bindStatus = 1 and (b.deleted is null or b.deleted = 0)")
    Optional<MonitorPartitionBindEntity> findActiveByDataReference(String dataReference);
}
