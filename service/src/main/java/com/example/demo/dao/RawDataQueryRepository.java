package com.example.demo.dao;

import com.example.demo.entity.RawDataEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
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
            entity.setTopic(rs.getString("topic"));
            entity.setPartitionId(toInteger(rs.getObject("partition_id")));
            entity.setMonitorId(toLong(rs.getObject("monitor_id")));
            entity.setShaftFloorId(toLong(rs.getObject("shaft_floor_id")));
            entity.setDataReference(rs.getString("data_reference"));
            entity.setIedFullPath(rs.getString("ied_full_path"));
            entity.setCollectTime(rs.getTimestamp("collect_time") == null ? null : rs.getTimestamp("collect_time").toLocalDateTime());
            entity.setMaxTemp(rs.getBigDecimal("max_temp"));
            entity.setMinTemp(rs.getBigDecimal("min_temp"));
            entity.setAvgTemp(rs.getBigDecimal("avg_temp"));
            entity.setMaxTempPosition(rs.getBigDecimal("max_temp_position"));
            entity.setMinTempPosition(rs.getBigDecimal("min_temp_position"));
            entity.setMaxTempChannel(toInteger(rs.getObject("max_temp_channel")));
            entity.setMinTempChannel(toInteger(rs.getObject("min_temp_channel")));
            entity.setPayloadJson(rs.getString("payload_json"));
            entity.setDeleted(toInteger(rs.getObject("deleted")));
            entity.setCreatedOn(rs.getTimestamp("created_on") == null ? null : rs.getTimestamp("created_on").toLocalDateTime());
            return entity;
        }
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RawDataQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RawDataEntity> query(
        Long monitorId,
        Long deviceId,
        Long shaftFloorId,
        Integer partitionId,
        LocalDateTime from,
        LocalDateTime to,
        int limit
    ) {
        String sql =
            "select id, device_id, iot_code, topic, partition_id, monitor_id, shaft_floor_id, data_reference, ied_full_path, " +
            "collect_time, max_temp, min_temp, avg_temp, max_temp_position, min_temp_position, max_temp_channel, min_temp_channel, " +
            "payload_json, deleted, created_on " +
            "from ODS_DWEQ_DM_RAW_DATA_D " +
            "where (deleted is null or deleted = 0) " +
            "and collect_time >= :from and collect_time <= :to " +
            "and (:monitorId is null or monitor_id = :monitorId) " +
            "and (:deviceId is null or device_id = :deviceId) " +
            "and (:shaftFloorId is null or shaft_floor_id = :shaftFloorId) " +
            "and (:partitionId is null or partition_id = :partitionId) " +
            "order by collect_time desc, id desc " +
            "limit :limit";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("monitorId", monitorId)
            .addValue("deviceId", deviceId)
            .addValue("shaftFloorId", shaftFloorId)
            .addValue("partitionId", partitionId)
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
