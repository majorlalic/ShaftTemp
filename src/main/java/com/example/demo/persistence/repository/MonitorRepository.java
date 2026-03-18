package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.MonitorEntity;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MonitorRepository extends JpaRepository<MonitorEntity, Long> {

    @Query("select m from MonitorEntity m where m.deviceId = ?1 and (m.deleted is null or m.deleted = 0)")
    Optional<MonitorEntity> findActiveByDeviceId(Long deviceId);

    @Query("select m from MonitorEntity m where m.id = ?1 and (m.deleted is null or m.deleted = 0)")
    Optional<MonitorEntity> findActiveById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MonitorEntity m where m.id = ?1 and (m.deleted is null or m.deleted = 0)")
    Optional<MonitorEntity> lockActiveById(Long id);

    @Query("select m from MonitorEntity m where (m.deleted is null or m.deleted = 0)")
    List<MonitorEntity> findAllActive();
}
