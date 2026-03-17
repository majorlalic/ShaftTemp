package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AlarmEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    @Query(
        "select a from AlarmEntity a where a.monitorId = ?1 and ((?2 is null and a.partitionCode is null) or a.partitionCode = ?2) " +
        "and a.alarmType = ?3 and a.status in ('ACTIVE', 'CONFIRMED') and (a.deleted is null or a.deleted = 0)"
    )
    Optional<AlarmEntity> findOpenAlarm(Long monitorId, String partitionCode, String alarmType);
}
