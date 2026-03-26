package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.RawDataEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RawDataRepository {

    @Select(
        "select * from raw_data " +
        "where (deleted is null or deleted = 0) " +
        "order by collect_time desc, id desc"
    )
    List<RawDataEntity> findRecentAll();
}
