package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.DeviceRawDataEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceRawDataRepository {

    @Select(
        "select * from device_raw_data " +
        "where (deleted is null or deleted = 0) " +
        "and collect_time >= #{from} and collect_time <= #{to} " +
        "order by collect_time desc, id desc"
    )
    List<DeviceRawDataEntity> findByCollectTimeBetweenOrderByCollectTimeDesc(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
