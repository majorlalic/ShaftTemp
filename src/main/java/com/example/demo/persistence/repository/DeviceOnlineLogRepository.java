package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.DeviceOnlineLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceOnlineLogRepository extends JpaRepository<DeviceOnlineLogEntity, Long> {
}
