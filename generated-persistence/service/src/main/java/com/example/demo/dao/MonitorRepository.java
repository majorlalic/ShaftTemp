package com.example.demo.dao;

import com.example.demo.entity.MonitorEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MonitorRepository {

    @Select("select * from ODS_DWEQ_DM_MONITOR_D where device_id = #{deviceId} and (deleted is null or deleted = 0)")
    Optional<MonitorEntity> findActiveByDeviceId(Long deviceId);

    @Select("select * from ODS_DWEQ_DM_MONITOR_D where id = #{id} and (deleted is null or deleted = 0)")
    Optional<MonitorEntity> findActiveById(Long id);

    @Select("select * from ODS_DWEQ_DM_MONITOR_D where (deleted is null or deleted = 0)")
    List<MonitorEntity> findAllActive();
}
