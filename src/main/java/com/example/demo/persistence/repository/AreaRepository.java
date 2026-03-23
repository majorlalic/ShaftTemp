package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.AreaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AreaRepository extends JpaRepository<AreaEntity, Long> {

    @Query("select a from AreaEntity a where a.deleted is null or a.deleted = 0 order by a.sort asc, a.id asc")
    List<AreaEntity> findAllActive();
}
