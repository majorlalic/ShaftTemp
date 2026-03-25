package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.DeviceRawDataEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeviceRawDataRepository extends JpaRepository<DeviceRawDataEntity, Long> {

    @Query(
        "select d from DeviceRawDataEntity d " +
        "where (d.deleted is null or d.deleted = 0) " +
        "and d.collectTime >= ?1 and d.collectTime <= ?2 " +
        "order by d.collectTime desc, d.id desc"
    )
    List<DeviceRawDataEntity> findByCollectTimeBetweenOrderByCollectTimeDesc(LocalDateTime from, LocalDateTime to);
}
