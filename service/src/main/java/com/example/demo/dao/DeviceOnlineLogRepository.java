package com.example.demo.dao;

import com.example.demo.entity.DeviceOnlineLogEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceOnlineLogRepository {

    @Insert({
        "insert into ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D (id, device_id, status, change_time, reason, deleted, created_on) values (",
        "#{id}, #{deviceId}, #{status}, #{changeTime}, #{reason}, #{deleted}, #{createdOn}",
        ")"
    })
    int insert(DeviceOnlineLogEntity entity);
}
