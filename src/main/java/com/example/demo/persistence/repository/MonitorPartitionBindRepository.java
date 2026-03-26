package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.MonitorPartitionBindEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MonitorPartitionBindRepository {

    @Select("select * from monitor_partition_bind where partition_code = #{partitionCode} and bind_status = 1 and (deleted is null or deleted = 0) limit 1")
    Optional<MonitorPartitionBindEntity> findActiveByPartitionCode(String partitionCode);

    @Select("select * from monitor_partition_bind where data_reference = #{dataReference} and bind_status = 1 and (deleted is null or deleted = 0) limit 1")
    Optional<MonitorPartitionBindEntity> findActiveByDataReference(String dataReference);

    @Select("select * from monitor_partition_bind where bind_status = 1 and (deleted is null or deleted = 0)")
    List<MonitorPartitionBindEntity> findAllActive();

    @Select("select * from monitor_partition_bind where monitor_id = #{monitorId} and bind_status = 1 and (deleted is null or deleted = 0) order by partition_no asc, id asc")
    List<MonitorPartitionBindEntity> findAllActiveByMonitorId(Long monitorId);

    @Select("select * from monitor_partition_bind where device_id = #{deviceId} and bind_status = 1 and (deleted is null or deleted = 0) order by partition_no asc, id asc")
    List<MonitorPartitionBindEntity> findAllActiveByDeviceId(Long deviceId);
}
