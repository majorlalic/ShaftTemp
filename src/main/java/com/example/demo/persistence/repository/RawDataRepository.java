package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.RawDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawDataRepository extends JpaRepository<RawDataEntity, Long> {
}
