package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.TempStatMinuteEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TempStatMinuteRepository {

    @Select(
        "select * from temp_stat_minute where device_id = #{deviceId} and monitor_id = #{monitorId} and " +
        "((#{partitionCode} is null and partition_code is null) or partition_code = #{partitionCode}) and " +
        "stat_time = #{statTime} and (deleted is null or deleted = 0) limit 1"
    )
    Optional<TempStatMinuteEntity> findActiveByStatTime(
        @Param("deviceId") Long deviceId,
        @Param("monitorId") Long monitorId,
        @Param("partitionCode") String partitionCode,
        @Param("statTime") LocalDateTime statTime
    );

    @Select("select * from temp_stat_minute where (deleted is null or deleted = 0) order by stat_time desc")
    List<TempStatMinuteEntity> findRecentAll();

    @Insert({
        "insert into temp_stat_minute (id, device_id, monitor_id, shaft_floor_id, partition_code, partition_name, data_reference, ",
        "device_token, partition_no, source_format, stat_time, max_temp, min_temp, avg_temp, alarm_point_count, deleted, created_on) values (",
        "#{id}, #{deviceId}, #{monitorId}, #{shaftFloorId}, #{partitionCode}, #{partitionName}, #{dataReference}, ",
        "#{deviceToken}, #{partitionNo}, #{sourceFormat}, #{statTime}, #{maxTemp}, #{minTemp}, #{avgTemp}, #{alarmPointCount}, #{deleted}, #{createdOn}",
        ")"
    })
    int insert(TempStatMinuteEntity entity);

    @Update({
        "update temp_stat_minute set",
        "device_id = #{deviceId},",
        "monitor_id = #{monitorId},",
        "shaft_floor_id = #{shaftFloorId},",
        "partition_code = #{partitionCode},",
        "partition_name = #{partitionName},",
        "data_reference = #{dataReference},",
        "device_token = #{deviceToken},",
        "partition_no = #{partitionNo},",
        "source_format = #{sourceFormat},",
        "stat_time = #{statTime},",
        "max_temp = #{maxTemp},",
        "min_temp = #{minTemp},",
        "avg_temp = #{avgTemp},",
        "alarm_point_count = #{alarmPointCount},",
        "deleted = #{deleted},",
        "created_on = #{createdOn}",
        "where id = #{id}"
    })
    int updateById(TempStatMinuteEntity entity);
}
