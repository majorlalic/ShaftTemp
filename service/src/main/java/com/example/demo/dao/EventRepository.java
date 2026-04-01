package com.example.demo.dao;

import com.example.demo.entity.EventEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EventRepository {

    @Select("select * from ODS_DWEQ_DM_EVENT_D where (deleted is null or deleted = 0)")
    List<EventEntity> findAll();

    @Select("select * from ODS_DWEQ_DM_EVENT_D where id = #{id}")
    Optional<EventEntity> findById(Long id);

    @Select("select * from ODS_DWEQ_DM_EVENT_D where alarm_id = #{alarmId} and (deleted is null or deleted = 0) order by event_time desc")
    List<EventEntity> findByAlarmIdOrderByEventTimeDesc(Long alarmId);

    @Insert({
        "insert into ODS_DWEQ_DM_EVENT_D (",
        "id, alarm_id, alarm_type, source_type, monitor_id, device_id, shaft_floor_id, partition_code, partition_name, ",
        "data_reference, device_token, partition_no, source_format, event_type, event_time, event_no, event_level, ",
        "point_list_json, detail_json, content, merged_flag, deleted, created_on, updated_on",
        ") values (",
        "#{id}, #{alarmId}, #{alarmType}, #{sourceType}, #{monitorId}, #{deviceId}, #{shaftFloorId}, #{partitionCode}, #{partitionName}, ",
        "#{dataReference}, #{deviceToken}, #{partitionNo}, #{sourceFormat}, #{eventType}, #{eventTime}, #{eventNo}, #{eventLevel}, ",
        "#{pointListJson}, #{detailJson}, #{content}, #{mergedFlag}, #{deleted}, #{createdOn}, #{updatedOn}",
        ")"
    })
    int insert(EventEntity entity);
}
