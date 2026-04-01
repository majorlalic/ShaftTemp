package com.example.demo.dao;

import com.example.demo.entity.AlarmRawEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlarmRawRepository {

    @Insert({
        "insert into ODS_DWEQ_DM_ALARM_RAW_D (",
        "id, iot_code, topic, partition_id, alarm_status, fault_status, ied_full_path, data_reference, collect_time, payload_json, deleted, created_on",
        ") values (",
        "#{id}, #{iotCode}, #{topic}, #{partitionId}, #{alarmStatus}, #{faultStatus}, #{iedFullPath}, #{dataReference}, #{collectTime}, #{payloadJson}, #{deleted}, #{createdOn}",
        ")"
    })
    int insert(AlarmRawEntity entity);
}

