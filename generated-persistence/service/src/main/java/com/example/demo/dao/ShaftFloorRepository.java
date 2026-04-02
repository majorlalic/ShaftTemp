package com.example.demo.dao;

import com.example.demo.entity.ShaftFloorEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShaftFloorRepository {

    @Select("select * from ODS_DWEQ_DM_SHAFT_FLOOR_D where id = #{id} and (deleted is null or deleted = 0)")
    Optional<ShaftFloorEntity> findActiveById(Long id);

    @Select("select * from ODS_DWEQ_DM_SHAFT_FLOOR_D where monitor_id = #{monitorId} and (deleted is null or deleted = 0) order by sort asc, id asc")
    List<ShaftFloorEntity> findAllActiveByMonitorId(Long monitorId);

    @Select("select * from ODS_DWEQ_DM_SHAFT_FLOOR_D where device_id = #{deviceId} and (deleted is null or deleted = 0) order by sort asc, id asc")
    List<ShaftFloorEntity> findAllActiveByDeviceId(Long deviceId);
}
