package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.OrgEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrgRepository extends JpaRepository<OrgEntity, Long> {

    @Query("select o from OrgEntity o where o.deleted is null or o.deleted = 0 order by o.sort asc, o.id asc")
    List<OrgEntity> findAllActive();
}
