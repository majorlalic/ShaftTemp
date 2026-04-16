package com.example.demo.dao;

import com.example.demo.entity.EventEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

    @Select({
        "<script>",
        "select count(*)",
        "from ODS_DWEQ_DM_EVENT_D e",
        "join ODS_DWEQ_DM_ALARM_D a on a.id = e.alarm_id",
        "left join ODS_DWEQ_DM_DEVICE_D d on d.id = cast(a.device_id as unsigned)",
        "left join ODS_DWEQ_DM_AREA_D ar on ar.id = d.area_id",
        "where (e.deleted is null or e.deleted = 0)",
        "and (a.deleted is null or a.deleted = 0)",
        "and (d.deleted is null or d.deleted = 0)",
        "and (ar.deleted is null or ar.deleted = 0)",
        "and a.alarm_type in ('DEVICE_OFFLINE', 'PARTITION_FAULT')",
        "<if test='statusCode != null'>",
        "and a.status = #{statusCode}",
        "</if>",
        "<if test='areaName != null and areaName != \"\"'>",
        "and ar.name like '%' || #{areaName} || '%'",
        "</if>",
        "<if test='deviceType != null and deviceType != \"\"'>",
        "and d.device_type = #{deviceType}",
        "</if>",
        "<if test='startTime != null'>",
        "and e.event_time &gt;= #{startTime}",
        "</if>",
        "<if test='endTime != null'>",
        "and e.event_time &lt;= #{endTime}",
        "</if>",
        "</script>"
    })
    Long countTerminalAlarmEventRows(
        @Param("statusCode") Integer statusCode,
        @Param("areaName") String areaName,
        @Param("deviceType") String deviceType,
        @Param("startTime") java.time.LocalDateTime startTime,
        @Param("endTime") java.time.LocalDateTime endTime
    );

    @Select({
        "<script>",
        "select * from (",
        "  select t.*, row_number() over(order by t.event_time desc, t.event_id desc) rn",
        "  from (",
        "    select",
        "      a.id as alarm_id,",
        "      e.id as event_id,",
        "      a.device_id as device_id,",
        "      d.name as device_name,",
        "      ar.name as area_name,",
        "      d.device_type as device_type,",
        "      a.alarm_type as alarm_type,",
        "      a.status as status_code,",
        "      a.first_alarm_time as first_alarm_time,",
        "      a.last_alarm_time as last_alarm_time,",
        "      a.event_count as alarm_count,",
        "      e.event_no as event_no,",
        "      e.event_time as event_time,",
        "      e.event_type as event_type,",
        "      case when e.content is not null then e.content else a.content end as alarm_content",
        "    from ODS_DWEQ_DM_EVENT_D e",
        "    join ODS_DWEQ_DM_ALARM_D a on a.id = e.alarm_id",
        "    left join ODS_DWEQ_DM_DEVICE_D d on d.id = cast(a.device_id as unsigned)",
        "    left join ODS_DWEQ_DM_AREA_D ar on ar.id = d.area_id",
        "    where (e.deleted is null or e.deleted = 0)",
        "      and (a.deleted is null or a.deleted = 0)",
        "      and (d.deleted is null or d.deleted = 0)",
        "      and (ar.deleted is null or ar.deleted = 0)",
        "      and a.alarm_type in ('DEVICE_OFFLINE', 'PARTITION_FAULT')",
        "      <if test='statusCode != null'>",
        "      and a.status = #{statusCode}",
        "      </if>",
        "      <if test='areaName != null and areaName != \"\"'>",
        "      and ar.name like '%' || #{areaName} || '%'",
        "      </if>",
        "      <if test='deviceType != null and deviceType != \"\"'>",
        "      and d.device_type = #{deviceType}",
        "      </if>",
        "      <if test='startTime != null'>",
        "      and e.event_time &gt;= #{startTime}",
        "      </if>",
        "      <if test='endTime != null'>",
        "      and e.event_time &lt;= #{endTime}",
        "      </if>",
        "  ) t",
        ") x",
        "where x.rn &gt;= #{startRow} and x.rn &lt;= #{endRow}",
        "order by x.rn",
        "</script>"
    })
    List<Map<String, Object>> findTerminalAlarmEventPage(
        @Param("statusCode") Integer statusCode,
        @Param("areaName") String areaName,
        @Param("deviceType") String deviceType,
        @Param("startTime") java.time.LocalDateTime startTime,
        @Param("endTime") java.time.LocalDateTime endTime,
        @Param("startRow") Integer startRow,
        @Param("endRow") Integer endRow
    );
}
