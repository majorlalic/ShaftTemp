package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.ShaftFloorEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShaftFloorRepository {

    @Select("select * from shaft_floor where id = #{id} and (deleted is null or deleted = 0) limit 1")
    Optional<ShaftFloorEntity> findActiveById(Long id);

    @Select("select * from shaft_floor where monitor_id = #{monitorId} and (deleted is null or deleted = 0) order by sort asc, id asc")
    List<ShaftFloorEntity> findAllActiveByMonitorId(Long monitorId);

    @Select("select * from shaft_floor where device_id = #{deviceId} and (deleted is null or deleted = 0) order by sort asc, id asc")
    List<ShaftFloorEntity> findAllActiveByDeviceId(Long deviceId);
}
