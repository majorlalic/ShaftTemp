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
            "id, device_id, iot_code, monitor_id, shaft_floor_id, partition_code, partition_name, data_reference, " +
            "device_token, partition_no, source_format, collect_time, point_count, valid_start_point, valid_end_point, " +
            "values_json, max_temp, min_temp, avg_temp, abnormal_flag, deleted, created_on" +
            ") values (" +
            ":id, :deviceId, :iotCode, :monitorId, :shaftFloorId, :partitionCode, :partitionName, :dataReference, " +
            ":deviceToken, :partitionNo, :sourceFormat, :collectTime, :pointCount, :validStartPoint, :validEndPoint, " +
            ":valuesJson, :maxTemp, :minTemp, :avgTemp, :abnormalFlag, :deleted, :createdOn" +
            ")";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", entity.getId())
            .addValue("deviceId", entity.getDeviceId())
            .addValue("iotCode", entity.getIotCode())
            .addValue("monitorId", entity.getMonitorId())
            .addValue("shaftFloorId", entity.getShaftFloorId())
            .addValue("partitionCode", entity.getPartitionCode())
            .addValue("partitionName", entity.getPartitionName())
            .addValue("dataReference", entity.getDataReference())
            .addValue("deviceToken", entity.getDeviceToken())
            .addValue("partitionNo", entity.getPartitionNo())
            .addValue("sourceFormat", entity.getSourceFormat())
            .addValue("collectTime", entity.getCollectTime())
            .addValue("pointCount", entity.getPointCount())
            .addValue("validStartPoint", entity.getValidStartPoint())
            .addValue("validEndPoint", entity.getValidEndPoint())
            .addValue("valuesJson", entity.getValuesJson())
            .addValue("maxTemp", entity.getMaxTemp())
            .addValue("minTemp", entity.getMinTemp())
            .addValue("avgTemp", entity.getAvgTemp())
            .addValue("abnormalFlag", entity.getAbnormalFlag())
            .addValue("deleted", entity.getDeleted())
            .addValue("createdOn", entity.getCreatedOn());
        return jdbcTemplate.update(sql, params);
    }
}
