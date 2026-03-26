package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AreaEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AreaRepository {

    @Select("select * from area where deleted is null or deleted = 0 order by sort asc, id asc")
    List<AreaEntity> findAllActive();

    @Insert({
        "insert into area (id, parent_id, name, type, path_ids, path_names, sort, deleted) values (",
        "#{id}, #{parentId}, #{name}, #{type}, #{pathIds}, #{pathNames}, #{sort}, #{deleted}",
        ") on duplicate key update ",
        "parent_id = values(parent_id),",
        "name = values(name),",
        "type = values(type),",
        "path_ids = values(path_ids),",
        "path_names = values(path_names),",
        "sort = values(sort),",
        "deleted = values(deleted)"
    })
    int upsert(AreaEntity entity);
}
