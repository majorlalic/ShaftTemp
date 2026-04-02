package com.example.demo.dao;

import com.example.demo.entity.AlarmEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlarmRepository {

    @Select("select * from ODS_DWEQ_DM_ALARM_D where id = #{id}")
    Optional<AlarmEntity> findById(Long id);

    @Select("select * from ODS_DWEQ_DM_ALARM_D where (deleted is null or deleted = 0)")
    List<AlarmEntity> findAll();

    @Select("select * from ODS_DWEQ_DM_ALARM_D where merge_key = #{mergeKey} and (deleted is null or deleted = 0)")
    Optional<AlarmEntity> findByMergeKey(String mergeKey);

    @Insert({
        "insert into ODS_DWEQ_DM_ALARM_D (",
        "id, alarm_code, alarm_type, source_type, monitor_id, device_id, shaft_floor_id, partition_code, partition_name, ",
        "data_reference, device_token, partition_no, source_format, merge_key, status, first_alarm_time, last_alarm_time, ",
        "merge_count, event_count, alarm_level, title, content, deleted, created_on, updated_on",
        ") values (",
        "#{id}, #{alarmCode}, #{alarmType}, #{sourceType}, #{monitorId}, #{deviceId}, #{shaftFloorId}, #{partitionCode}, #{partitionName}, ",
        "#{dataReference}, #{deviceToken}, #{partitionNo}, #{sourceFormat}, #{mergeKey}, #{status}, #{firstAlarmTime}, #{lastAlarmTime}, ",
        "#{mergeCount}, #{eventCount}, #{alarmLevel}, #{title}, #{content}, #{deleted}, #{createdOn}, #{updatedOn}",
        ")"
    })
    int upsertPendingAlarm(AlarmEntity entity);

    @Update({
        "<script>",
        "update ODS_DWEQ_DM_ALARM_D",
        "set alarm_code = #{alarmCode},",
        "alarm_type = #{alarmType},",
        "source_type = #{sourceType},",
        "monitor_id = #{monitorId},",
        "device_id = #{deviceId},",
        "shaft_floor_id = #{shaftFloorId},",
        "partition_code = #{partitionCode},",
        "partition_name = #{partitionName},",
        "data_reference = #{dataReference},",
        "device_token = #{deviceToken},",
        "partition_no = #{partitionNo},",
        "source_format = #{sourceFormat},",
        "merge_key = #{mergeKey},",
        "status = #{status},",
        "first_alarm_time = #{firstAlarmTime},",
        "last_alarm_time = #{lastAlarmTime},",
        "merge_count = #{mergeCount},",
        "event_count = #{eventCount},",
        "alarm_level = #{alarmLevel},",
        "title = #{title},",
        "content = #{content},",
        "handler = #{handler},",
        "handle_time = #{handleTime},",
        "handle_remark = #{handleRemark},",
        "deleted = #{deleted},",
        "created_on = #{createdOn},",
        "updated_on = #{updatedOn}",
        "where id = #{id}",
        "</script>"
    })
    int updateById(AlarmEntity entity);
}
