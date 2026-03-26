package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.EventEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EventRepository {

    @Select("select * from event where (deleted is null or deleted = 0)")
    List<EventEntity> findAll();

    @Select("select * from event where id = #{id} limit 1")
    Optional<EventEntity> findById(Long id);

    @Select("select * from event where alarm_id = #{alarmId} and (deleted is null or deleted = 0) order by event_time desc")
    List<EventEntity> findByAlarmIdOrderByEventTimeDesc(Long alarmId);
}
