package com.example.demo.persistence.repository;

import com.example.demo.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
}
