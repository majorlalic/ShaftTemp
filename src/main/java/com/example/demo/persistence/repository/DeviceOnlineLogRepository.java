package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.DeviceOnlineLogEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceOnlineLogRepository {

    @Insert({
        "insert into device_online_log (id, device_id, status, change_time, reason, deleted, created_on) values (",
        "#{id}, #{deviceId}, #{status}, #{changeTime}, #{reason}, #{deleted}, #{createdOn}",
        ")"
    })
    int insert(DeviceOnlineLogEntity entity);
}
