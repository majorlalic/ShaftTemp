package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AlarmEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlarmRepository {

    @Select("select * from alarm where id = #{id}")
    Optional<AlarmEntity> findById(Long id);

    @Select("select * from alarm where (deleted is null or deleted = 0)")
    List<AlarmEntity> findAll();

    @Select("select * from alarm where merge_key = #{mergeKey} and (deleted is null or deleted = 0) limit 1")
    Optional<AlarmEntity> findByMergeKey(String mergeKey);

    @Update({
        "<script>",
        "update alarm",
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
