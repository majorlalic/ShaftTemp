package com.example.demo.dao;

import com.example.demo.entity.RawDataEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RawDataRepository {

    @Insert({
        "insert into ODS_DWEQ_DM_RAW_DATA_D (",
        "id, device_id, iot_code, topic, partition_id, monitor_id, shaft_floor_id, data_reference, ied_full_path, ",
        "collect_time, max_temp, min_temp, avg_temp, max_temp_position, min_temp_position, max_temp_channel, min_temp_channel, ",
        "payload_json, deleted, created_on",
        ") values (",
        "#{id}, #{deviceId}, #{iotCode}, #{topic}, #{partitionId}, #{monitorId}, #{shaftFloorId}, #{dataReference}, #{iedFullPath}, ",
        "#{collectTime}, #{maxTemp}, #{minTemp}, #{avgTemp}, #{maxTempPosition}, #{minTempPosition}, #{maxTempChannel}, #{minTempChannel}, ",
        "#{payloadJson}, #{deleted}, #{createdOn}",
        ")"
    })
    int insert(RawDataEntity entity);
}

