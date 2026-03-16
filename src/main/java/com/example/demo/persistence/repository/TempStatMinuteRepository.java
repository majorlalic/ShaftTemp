package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.TempStatMinuteEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TempStatMinuteRepository extends JpaRepository<TempStatMinuteEntity, Long> {

    @Query("select t from TempStatMinuteEntity t where t.deviceId = ?1 and t.monitorId = ?2 and t.statTime = ?3 and (t.deleted is null or t.deleted = 0)")
    Optional<TempStatMinuteEntity> findActiveByStatTime(Long deviceId, Long monitorId, LocalDateTime statTime);

    @Query("select t from TempStatMinuteEntity t where (t.deleted is null or t.deleted = 0) order by t.statTime desc")
    List<TempStatMinuteEntity> findRecentAll();
}
