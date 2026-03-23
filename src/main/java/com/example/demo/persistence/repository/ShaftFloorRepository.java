package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.ShaftFloorEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShaftFloorRepository extends JpaRepository<ShaftFloorEntity, Long> {

    @Query("select f from ShaftFloorEntity f where f.id = ?1 and (f.deleted is null or f.deleted = 0)")
    Optional<ShaftFloorEntity> findActiveById(Long id);

    @Query("select f from ShaftFloorEntity f where f.monitorId = ?1 and (f.deleted is null or f.deleted = 0) order by f.sort asc, f.id asc")
    List<ShaftFloorEntity> findAllActiveByMonitorId(Long monitorId);

    @Query("select f from ShaftFloorEntity f where f.deviceId = ?1 and (f.deleted is null or f.deleted = 0) order by f.sort asc, f.id asc")
    List<ShaftFloorEntity> findAllActiveByDeviceId(Long deviceId);
}
