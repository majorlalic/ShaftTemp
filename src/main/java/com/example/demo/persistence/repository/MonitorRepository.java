package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.MonitorEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MonitorRepository {

    @Select("select * from monitor where device_id = #{deviceId} and (deleted is null or deleted = 0) limit 1")
    Optional<MonitorEntity> findActiveByDeviceId(Long deviceId);

    @Select("select * from monitor where id = #{id} and (deleted is null or deleted = 0) limit 1")
    Optional<MonitorEntity> findActiveById(Long id);

    @Select("select * from monitor where (deleted is null or deleted = 0)")
    List<MonitorEntity> findAllActive();
}
