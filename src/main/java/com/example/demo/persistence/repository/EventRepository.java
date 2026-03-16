package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.EventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    @Query("select e from EventEntity e where e.alarmId = ?1 and (e.deleted is null or e.deleted = 0) order by e.eventTime desc")
    List<EventEntity> findByAlarmIdOrderByEventTimeDesc(Long alarmId);
}
