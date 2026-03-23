package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.DeviceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    @Query("select d from DeviceEntity d where d.iotCode = ?1 and (d.deleted is null or d.deleted = 0)")
    Optional<DeviceEntity> findActiveByIotCode(String iotCode);

    @Query("select d from DeviceEntity d where d.id = ?1 and (d.deleted is null or d.deleted = 0)")
    Optional<DeviceEntity> findActiveById(Long id);

    @Query("select d from DeviceEntity d where d.deleted is null or d.deleted = 0")
    List<DeviceEntity> findAllActive();

    @Query("select d from DeviceEntity d where d.assetStatus = ?1 and (d.deleted is null or d.deleted = 0)")
    List<DeviceEntity> findAllActiveByAssetStatus(String assetStatus);
}
