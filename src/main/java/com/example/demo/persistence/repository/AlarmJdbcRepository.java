package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AlarmEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlarmJdbcRepository {

    private static final int MAX_RETRIES = 3;

    private static final String UPSERT_SQL =
        "insert into alarm (" +
            "id, alarm_code, alarm_type, source_type, monitor_id, device_id, shaft_floor_id, partition_code, partition_name, " +
            "data_reference, device_token, partition_no, source_format, merge_key, status, first_alarm_time, last_alarm_time, " +
            "merge_count, event_count, alarm_level, title, content, deleted, created_on, updated_on" +
        ") values (" +
            ":id, :alarmCode, :alarmType, :sourceType, :monitorId, :deviceId, :shaftFloorId, :partitionCode, :partitionName, " +
            ":dataReference, :deviceToken, :partitionNo, :sourceFormat, :mergeKey, :status, :firstAlarmTime, :lastAlarmTime, " +
            ":mergeCount, :eventCount, :alarmLevel, :title, :content, :deleted, :createdOn, :updatedOn" +
        ") on duplicate key update " +
            "source_type = values(source_type), " +
            "device_id = values(device_id), " +
            "shaft_floor_id = values(shaft_floor_id), " +
            "partition_code = values(partition_code), " +
            "partition_name = values(partition_name), " +
            "data_reference = values(data_reference), " +
            "device_token = values(device_token), " +
            "partition_no = values(partition_no), " +
            "source_format = values(source_format), " +
            "last_alarm_time = values(last_alarm_time), " +
            "merge_count = ifnull(merge_count, 0) + 1, " +
            "alarm_level = values(alarm_level), " +
            "title = values(title), " +
            "content = values(content), " +
            "updated_on = values(updated_on)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AlarmJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean upsertPendingAlarm(AlarmEntity alarm) {
        int attempt = 0;
        while (true) {
            try {
                return jdbcTemplate.update(UPSERT_SQL, toParams(alarm)) == 1;
            } catch (DeadlockLoserDataAccessException | CannotAcquireLockException ex) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    throw ex;
                }
                try {
                    Thread.sleep(5L * attempt);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
    }

    private Map<String, Object> toParams(AlarmEntity alarm) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("id", alarm.getId());
        params.put("alarmCode", alarm.getAlarmCode());
        params.put("alarmType", alarm.getAlarmType());
        params.put("sourceType", alarm.getSourceType());
        params.put("monitorId", alarm.getMonitorId());
        params.put("deviceId", alarm.getDeviceId());
        params.put("shaftFloorId", alarm.getShaftFloorId());
        params.put("partitionCode", alarm.getPartitionCode());
        params.put("partitionName", alarm.getPartitionName());
        params.put("dataReference", alarm.getDataReference());
        params.put("deviceToken", alarm.getDeviceToken());
        params.put("partitionNo", alarm.getPartitionNo());
        params.put("sourceFormat", alarm.getSourceFormat());
        params.put("mergeKey", alarm.getMergeKey());
        params.put("status", alarm.getStatus());
        params.put("firstAlarmTime", alarm.getFirstAlarmTime());
        params.put("lastAlarmTime", alarm.getLastAlarmTime());
        params.put("mergeCount", alarm.getMergeCount());
        params.put("eventCount", alarm.getEventCount());
        params.put("alarmLevel", alarm.getAlarmLevel());
        params.put("title", alarm.getTitle());
        params.put("content", alarm.getContent());
        params.put("deleted", alarm.getDeleted());
        params.put("createdOn", alarm.getCreatedOn());
        params.put("updatedOn", alarm.getUpdatedOn());
        return params;
    }
}
