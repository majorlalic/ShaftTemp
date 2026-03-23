package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AlarmRuleEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlarmRuleRepository extends JpaRepository<AlarmRuleEntity, Long> {

    @Query("select r from AlarmRuleEntity r where (r.deleted is null or r.deleted = 0) order by r.updatedOn desc")
    List<AlarmRuleEntity> findAllActive();

    @Query("select r from AlarmRuleEntity r where r.id = ?1 and (r.deleted is null or r.deleted = 0)")
    Optional<AlarmRuleEntity> findActiveById(Long id);
}
