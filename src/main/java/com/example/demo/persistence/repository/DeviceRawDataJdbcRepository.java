package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.DeviceRawDataEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceRawDataJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DeviceRawDataJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(DeviceRawDataEntity entity) {
        String sql =
            "insert into device_raw_data (" +
            "id, device_id, iot_code, monitor_id, topic, ied_full_path, collect_time, point_count, " +
            "valid_start_point, valid_end_point, values_json, max_temp, min_temp, avg_temp, deleted, created_on" +
            ") values (" +
            ":id, :deviceId, :iotCode, :monitorId, :topic, :iedFullPath, :collectTime, :pointCount, " +
            ":validStartPoint, :validEndPoint, :valuesJson, :maxTemp, :minTemp, :avgTemp, :deleted, :createdOn" +
            ")";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", entity.getId())
            .addValue("deviceId", entity.getDeviceId())
            .addValue("iotCode", entity.getIotCode())
            .addValue("monitorId", entity.getMonitorId())
            .addValue("topic", entity.getTopic())
            .addValue("iedFullPath", entity.getIedFullPath())
            .addValue("collectTime", entity.getCollectTime())
            .addValue("pointCount", entity.getPointCount())
            .addValue("validStartPoint", entity.getValidStartPoint())
            .addValue("validEndPoint", entity.getValidEndPoint())
            .addValue("valuesJson", entity.getValuesJson())
            .addValue("maxTemp", entity.getMaxTemp())
            .addValue("minTemp", entity.getMinTemp())
            .addValue("avgTemp", entity.getAvgTemp())
            .addValue("deleted", entity.getDeleted())
            .addValue("createdOn", entity.getCreatedOn());
        return jdbcTemplate.update(sql, params);
    }
}
