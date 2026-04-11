package com.example.demo.dao;

import com.example.demo.entity.MonitorEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MonitorRepository {

    @Select("select * from ODS_DWEQ_DM_MONITOR_D where device_id = #{deviceId} and (deleted is null or deleted = 0)")
    Optional<MonitorEntity> findActiveByDeviceId(Long deviceId);

    @Select("select * from ODS_DWEQ_DM_MONITOR_D where id = #{id} and (deleted is null or deleted = 0)")
    Optional<MonitorEntity> findActiveById(Long id);

    @Select("select * from ODS_DWEQ_DM_MONITOR_D where (deleted is null or deleted = 0)")
    List<MonitorEntity> findAllActive();

    @Select({
        "select m.*",
        "from ODS_DWEQ_DM_MONITOR_D m",
        "join ODS_DWEQ_DM_AREA_D a on a.id = m.area_id",
        "where (m.deleted is null or m.deleted = 0)",
        "and (a.deleted is null or a.deleted = 0)",
        "and (a.id = #{areaTreeId} or instr('/' || nvl(a.path_ids, '') || '/', '/' || #{areaTreeId} || '/') > 0)",
        "order by m.id asc"
    })
    List<MonitorEntity> findAllActiveByAreaTreeId(@Param("areaTreeId") Long areaTreeId);

    @Select({
        "select count(distinct device_id) from ODS_DWEQ_DM_MONITOR_D",
        "where device_id is not null and (deleted is null or deleted = 0)"
    })
    Long countMonitorDeviceTotal();

    @Select({
        "select count(distinct m.device_id)",
        "from ODS_DWEQ_DM_MONITOR_D m",
        "join ODS_DWEQ_DM_AREA_D a on a.id = m.area_id",
        "where m.device_id is not null",
        "and (m.deleted is null or m.deleted = 0)",
        "and (a.deleted is null or a.deleted = 0)",
        "and (a.id = #{areaTreeId} or instr('/' || nvl(a.path_ids, '') || '/', '/' || #{areaTreeId} || '/') > 0)"
    })
    Long countMonitorDeviceTotalByAreaTree(@Param("areaTreeId") Long areaTreeId);
}
