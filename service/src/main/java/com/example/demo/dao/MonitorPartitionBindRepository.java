package com.example.demo.dao;

import com.example.demo.entity.MonitorPartitionBindEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MonitorPartitionBindRepository {

    @Select("select * from ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D where partition_code = #{partitionCode} and bind_status = 1 and (deleted is null or deleted = 0)")
    Optional<MonitorPartitionBindEntity> findActiveByPartitionCode(String partitionCode);

    @Select("select * from ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D where data_reference = #{dataReference} and bind_status = 1 and (deleted is null or deleted = 0)")
    Optional<MonitorPartitionBindEntity> findActiveByDataReference(String dataReference);

    @Select(
        "select * from ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D " +
        "where device_id = #{deviceId} and partition_id = #{partitionId} and bind_status = 1 and (deleted is null or deleted = 0)"
    )
    Optional<MonitorPartitionBindEntity> findActiveByDeviceAndPartitionId(
        @Param("deviceId") Long deviceId,
        @Param("partitionId") Integer partitionId
    );

    @Select("select * from ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D where bind_status = 1 and (deleted is null or deleted = 0)")
    List<MonitorPartitionBindEntity> findAllActive();

    @Select("select * from ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D where monitor_id = #{monitorId} and bind_status = 1 and (deleted is null or deleted = 0) order by partition_no asc, id asc")
    List<MonitorPartitionBindEntity> findAllActiveByMonitorId(Long monitorId);

    @Select({
        "<script>",
        "select * from ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D",
        "where bind_status = 1 and (deleted is null or deleted = 0)",
        "and monitor_id in",
        "<foreach collection='monitorIds' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "order by partition_no asc, id asc",
        "</script>"
    })
    List<MonitorPartitionBindEntity> findAllActiveByMonitorIds(@Param("monitorIds") List<Long> monitorIds);

    @Select("select * from ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D where device_id = #{deviceId} and bind_status = 1 and (deleted is null or deleted = 0) order by partition_no asc, id asc")
    List<MonitorPartitionBindEntity> findAllActiveByDeviceId(Long deviceId);

    @Select({
        "<script>",
        "select * from ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D",
        "where bind_status = 1 and (deleted is null or deleted = 0)",
        "and shaft_floor_id in",
        "<foreach collection='shaftFloorIds' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "order by partition_no asc, id asc",
        "</script>"
    })
    List<MonitorPartitionBindEntity> findAllActiveByShaftFloorIds(@Param("shaftFloorIds") List<Long> shaftFloorIds);
}
