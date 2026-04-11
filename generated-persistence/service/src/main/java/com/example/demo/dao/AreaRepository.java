package com.example.demo.dao;

import com.example.demo.entity.AreaEntity;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AreaRepository {

    @Select("select * from ODS_DWEQ_DM_AREA_D where deleted is null or deleted = 0 order by sort asc, id asc")
    List<AreaEntity> findAllActive();

    @Select("select * from ODS_DWEQ_DM_AREA_D where id = #{id} and (deleted is null or deleted = 0)")
    Optional<AreaEntity> findActiveById(Long id);

    @Update({
        "MERGE INTO ODS_DWEQ_DM_AREA_D t",
        "USING (SELECT #{id} id, #{parentId} parent_id, #{name} name, #{type} type, #{pathIds} path_ids, #{pathNames} path_names, #{sort} sort, #{deleted} deleted FROM dual) s",
        "ON (t.id = s.id)",
        "WHEN MATCHED THEN UPDATE SET",
        "t.parent_id = s.parent_id,",
        "t.name = s.name,",
        "t.type = s.type,",
        "t.path_ids = s.path_ids,",
        "t.path_names = s.path_names,",
        "t.sort = s.sort,",
        "t.deleted = s.deleted",
        "WHEN NOT MATCHED THEN INSERT (id, parent_id, name, type, path_ids, path_names, sort, deleted)",
        "VALUES (s.id, s.parent_id, s.name, s.type, s.path_ids, s.path_names, s.sort, s.deleted)"
    })
    int upsert(AreaEntity entity);
}
