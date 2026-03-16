package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AlarmEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    @Query("select a from AlarmEntity a where a.monitorId = ?1 and a.alarmType = ?2 and a.status = 'ACTIVE' and (a.deleted is null or a.deleted = 0)")
    Optional<AlarmEntity> findActiveAlarm(Long monitorId, String alarmType);
}
