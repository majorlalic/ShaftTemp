package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AlarmRawEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlarmRawJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AlarmRawJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(AlarmRawEntity entity) {
        String sql =
            "insert into alarm_raw (" +
            "id, iot_code, topic, partition_id, alarm_status, fault_status, ied_full_path, data_reference, collect_time, payload_json, deleted, created_on" +
            ") values (" +
            ":id, :iotCode, :topic, :partitionId, :alarmStatus, :faultStatus, :iedFullPath, :dataReference, :collectTime, :payloadJson, :deleted, :createdOn" +
            ")";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", entity.getId())
            .addValue("iotCode", entity.getIotCode())
            .addValue("topic", entity.getTopic())
            .addValue("partitionId", entity.getPartitionId())
            .addValue("alarmStatus", entity.getAlarmStatus())
            .addValue("faultStatus", entity.getFaultStatus())
            .addValue("iedFullPath", entity.getIedFullPath())
            .addValue("dataReference", entity.getDataReference())
            .addValue("collectTime", entity.getCollectTime())
            .addValue("payloadJson", entity.getPayloadJson())
            .addValue("deleted", entity.getDeleted())
            .addValue("createdOn", entity.getCreatedOn());
        return jdbcTemplate.update(sql, params);
    }
}
