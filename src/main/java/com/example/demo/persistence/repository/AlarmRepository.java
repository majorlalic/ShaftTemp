package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AlarmEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    @Query("select a from AlarmEntity a where a.mergeKey = ?1 and (a.deleted is null or a.deleted = 0)")
    Optional<AlarmEntity> findByMergeKey(String mergeKey);
}
