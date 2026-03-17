package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.ShaftFloorEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShaftFloorRepository extends JpaRepository<ShaftFloorEntity, Long> {

    @Query("select f from ShaftFloorEntity f where f.id = ?1 and (f.deleted is null or f.deleted = 0)")
    Optional<ShaftFloorEntity> findActiveById(Long id);
}
