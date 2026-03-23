package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.MonitorDeviceBindEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MonitorDeviceBindRepository extends JpaRepository<MonitorDeviceBindEntity, Long> {

    @Query("select b from MonitorDeviceBindEntity b where b.monitorId = ?1 and b.deviceId = ?2 and b.bindStatus = 1 and (b.deleted is null or b.deleted = 0)")
    Optional<MonitorDeviceBindEntity> findActiveByMonitorIdAndDeviceId(Long monitorId, Long deviceId);

    @Query("select b from MonitorDeviceBindEntity b where b.monitorId = ?1 and b.bindStatus = 1 and (b.deleted is null or b.deleted = 0)")
    List<MonitorDeviceBindEntity> findAllActiveByMonitorId(Long monitorId);

    @Query("select b from MonitorDeviceBindEntity b where b.deviceId = ?1 and b.bindStatus = 1 and (b.deleted is null or b.deleted = 0)")
    List<MonitorDeviceBindEntity> findAllActiveByDeviceId(Long deviceId);
}
