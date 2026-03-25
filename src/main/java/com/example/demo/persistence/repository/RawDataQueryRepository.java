package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.RawDataEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RawDataQueryRepository {

    private static final RowMapper<RawDataEntity> ROW_MAPPER = new RowMapper<RawDataEntity>() {
        @Override
        public RawDataEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            RawDataEntity entity = new RawDataEntity();
            entity.setId(rs.getLong("id"));
            entity.setDeviceId(toLong(rs.getObject("device_id")));
            entity.setIotCode(rs.getString("iot_code"));
            entity.setMonitorId(toLong(rs.getObject("monitor_id")));
            entity.setShaftFloorId(toLong(rs.getObject("shaft_floor_id")));
            entity.setPartitionCode(rs.getString("partition_code"));
            entity.setPartitionName(rs.getString("partition_name"));
            entity.setDataReference(rs.getString("data_reference"));
            entity.setDeviceToken(rs.getString("device_token"));
            entity.setPartitionNo(toInteger(rs.getObject("partition_no")));
            entity.setSourceFormat(rs.getString("source_format"));
            entity.setCollectTime(rs.getTimestamp("collect_time") == null ? null : rs.getTimestamp("collect_time").toLocalDateTime());
            entity.setPointCount(toInteger(rs.getObject("point_count")));
            entity.setValidStartPoint(toInteger(rs.getObject("valid_start_point")));
            entity.setValidEndPoint(toInteger(rs.getObject("valid_end_point")));
            entity.setValuesJson(rs.getString("values_json"));
            entity.setMaxTemp(rs.getBigDecimal("max_temp"));
            entity.setMinTemp(rs.getBigDecimal("min_temp"));
            entity.setAvgTemp(rs.getBigDecimal("avg_temp"));
            entity.setAbnormalFlag(toInteger(rs.getObject("abnormal_flag")));
            entity.setDeleted(toInteger(rs.getObject("deleted")));
            entity.setCreatedOn(rs.getTimestamp("created_on") == null ? null : rs.getTimestamp("created_on").toLocalDateTime());
            return entity;
        }
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RawDataTableRouter rawDataTableRouter;
    private final RawDataTableManager rawDataTableManager;

    public RawDataQueryRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        RawDataTableRouter rawDataTableRouter,
        RawDataTableManager rawDataTableManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rawDataTableRouter = rawDataTableRouter;
        this.rawDataTableManager = rawDataTableManager;
    }

    public List<RawDataEntity> query(
        Long monitorId,
        Long deviceId,
        Long shaftFloorId,
        String partitionCode,
        LocalDateTime from,
        LocalDateTime to,
        int limit
    ) {
        List<String> candidateTables = new ArrayList<String>();
        candidateTables.add("raw_data");
        candidateTables.addAll(rawDataTableRouter.resolveTables(from, to));
        List<String> tables = rawDataTableManager.existingTables(candidateTables.subList(1, candidateTables.size()));
        tables.add(0, "raw_data");

        List<String> selects = new ArrayList<String>();
        for (String table : tables) {
            selects.add(
                "select id, device_id, iot_code, monitor_id, shaft_floor_id, partition_code, partition_name, data_reference, " +
                "device_token, partition_no, source_format, collect_time, point_count, valid_start_point, valid_end_point, " +
                "values_json, max_temp, min_temp, avg_temp, abnormal_flag, deleted, created_on " +
                "from " + table +
                " where (deleted is null or deleted = 0) " +
                "and collect_time >= :from and collect_time <= :to " +
                "and (:monitorId is null or monitor_id = :monitorId) " +
                "and (:deviceId is null or device_id = :deviceId) " +
                "and (:shaftFloorId is null or shaft_floor_id = :shaftFloorId) " +
                "and (:partitionCode is null or partition_code = :partitionCode)"
            );
        }

        String sql =
            "select * from (" + String.join(" union all ", selects) + ") raw " +
            "order by collect_time desc, id desc limit :limit";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("monitorId", monitorId)
            .addValue("deviceId", deviceId)
            .addValue("shaftFloorId", shaftFloorId)
            .addValue("partitionCode", partitionCode)
            .addValue("from", from)
            .addValue("to", to)
            .addValue("limit", limit);
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Integer toInteger(Object value) {
        return value == null ? null : Integer.valueOf(((Number) value).intValue());
    }
}
