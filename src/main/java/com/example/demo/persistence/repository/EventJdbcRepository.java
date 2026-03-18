package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.EventEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EventJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(EventEntity entity) {
        String sql =
            "insert into event (" +
            "id, alarm_id, alarm_type, source_type, monitor_id, device_id, shaft_floor_id, partition_code, partition_name, " +
            "data_reference, device_token, partition_no, source_format, event_type, event_time, event_no, event_level, " +
            "point_list_json, detail_json, content, merged_flag, deleted, created_on, updated_on" +
            ") values (" +
            ":id, :alarmId, :alarmType, :sourceType, :monitorId, :deviceId, :shaftFloorId, :partitionCode, :partitionName, " +
            ":dataReference, :deviceToken, :partitionNo, :sourceFormat, :eventType, :eventTime, :eventNo, :eventLevel, " +
            ":pointListJson, :detailJson, :content, :mergedFlag, :deleted, :createdOn, :updatedOn" +
            ")";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", entity.getId())
            .addValue("alarmId", entity.getAlarmId())
            .addValue("alarmType", entity.getAlarmType())
            .addValue("sourceType", entity.getSourceType())
            .addValue("monitorId", entity.getMonitorId())
            .addValue("deviceId", entity.getDeviceId())
            .addValue("shaftFloorId", entity.getShaftFloorId())
            .addValue("partitionCode", entity.getPartitionCode())
            .addValue("partitionName", entity.getPartitionName())
            .addValue("dataReference", entity.getDataReference())
            .addValue("deviceToken", entity.getDeviceToken())
            .addValue("partitionNo", entity.getPartitionNo())
            .addValue("sourceFormat", entity.getSourceFormat())
            .addValue("eventType", entity.getEventType())
            .addValue("eventTime", entity.getEventTime())
            .addValue("eventNo", entity.getEventNo())
            .addValue("eventLevel", entity.getEventLevel())
            .addValue("pointListJson", entity.getPointListJson())
            .addValue("detailJson", entity.getDetailJson())
            .addValue("content", entity.getContent())
            .addValue("mergedFlag", entity.getMergedFlag())
            .addValue("deleted", entity.getDeleted())
            .addValue("createdOn", entity.getCreatedOn())
            .addValue("updatedOn", entity.getUpdatedOn());
        return jdbcTemplate.update(sql, params);
    }
}
