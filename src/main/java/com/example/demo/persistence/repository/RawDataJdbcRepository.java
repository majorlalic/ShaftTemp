package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.RawDataEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RawDataJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RawDataTableRouter rawDataTableRouter;
    private final RawDataTableManager rawDataTableManager;

    public RawDataJdbcRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        RawDataTableRouter rawDataTableRouter,
        RawDataTableManager rawDataTableManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rawDataTableRouter = rawDataTableRouter;
        this.rawDataTableManager = rawDataTableManager;
    }

    public int insert(RawDataEntity entity) {
        String tableName = rawDataTableRouter.resolveTable(entity.getCollectTime());
        rawDataTableManager.ensureTableExists(tableName);
        String sql =
            "insert into " + tableName + " (" +
            "id, device_id, iot_code, topic, partition_id, monitor_id, shaft_floor_id, data_reference, ied_full_path, " +
            "collect_time, max_temp, min_temp, avg_temp, max_temp_position, min_temp_position, max_temp_channel, min_temp_channel, " +
            "payload_json, deleted, created_on" +
            ") values (" +
            ":id, :deviceId, :iotCode, :topic, :partitionId, :monitorId, :shaftFloorId, :dataReference, :iedFullPath, " +
            ":collectTime, :maxTemp, :minTemp, :avgTemp, :maxTempPosition, :minTempPosition, :maxTempChannel, :minTempChannel, " +
            ":payloadJson, :deleted, :createdOn" +
            ")";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", entity.getId())
            .addValue("deviceId", entity.getDeviceId())
            .addValue("iotCode", entity.getIotCode())
            .addValue("topic", entity.getTopic())
            .addValue("partitionId", entity.getPartitionId())
            .addValue("monitorId", entity.getMonitorId())
            .addValue("shaftFloorId", entity.getShaftFloorId())
            .addValue("dataReference", entity.getDataReference())
            .addValue("iedFullPath", entity.getIedFullPath())
            .addValue("collectTime", entity.getCollectTime())
            .addValue("maxTemp", entity.getMaxTemp())
            .addValue("minTemp", entity.getMinTemp())
            .addValue("avgTemp", entity.getAvgTemp())
            .addValue("maxTempPosition", entity.getMaxTempPosition())
            .addValue("minTempPosition", entity.getMinTempPosition())
            .addValue("maxTempChannel", entity.getMaxTempChannel())
            .addValue("minTempChannel", entity.getMinTempChannel())
            .addValue("payloadJson", entity.getPayloadJson())
            .addValue("deleted", entity.getDeleted())
            .addValue("createdOn", entity.getCreatedOn());
        return jdbcTemplate.update(sql, params);
    }
}
