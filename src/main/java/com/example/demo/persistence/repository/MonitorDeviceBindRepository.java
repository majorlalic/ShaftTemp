package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.MonitorDeviceBindEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MonitorDeviceBindRepository {

    @Select("select * from monitor_device_bind where monitor_id = #{monitorId} and device_id = #{deviceId} and bind_status = 1 and (deleted is null or deleted = 0) limit 1")
    Optional<MonitorDeviceBindEntity> findActiveByMonitorIdAndDeviceId(@Param("monitorId") Long monitorId, @Param("deviceId") Long deviceId);

    @Select("select * from monitor_device_bind where monitor_id = #{monitorId} and bind_status = 1 and (deleted is null or deleted = 0)")
    List<MonitorDeviceBindEntity> findAllActiveByMonitorId(Long monitorId);

    @Select("select * from monitor_device_bind where device_id = #{deviceId} and bind_status = 1 and (deleted is null or deleted = 0)")
    List<MonitorDeviceBindEntity> findAllActiveByDeviceId(Long deviceId);

    @Insert({
        "insert into monitor_device_bind (id, monitor_id, device_id, bind_status, bind_time, unbind_time, deleted, created_on, updated_on) values (",
        "#{id}, #{monitorId}, #{deviceId}, #{bindStatus}, #{bindTime}, #{unbindTime}, #{deleted}, #{createdOn}, #{updatedOn}",
        ")"
    })
    int insert(MonitorDeviceBindEntity entity);
}
