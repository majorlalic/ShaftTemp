package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.RawDataEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RawDataRepository extends JpaRepository<RawDataEntity, Long> {

    @Query("select r from RawDataEntity r where (r.deleted is null or r.deleted = 0) order by r.collectTime desc")
    List<RawDataEntity> findRecentAll();
}
