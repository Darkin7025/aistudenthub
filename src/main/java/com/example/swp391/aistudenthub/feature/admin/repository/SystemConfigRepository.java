package com.example.swp391.aistudenthub.feature.admin.repository;

import com.example.swp391.aistudenthub.feature.admin.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    List<SystemConfig> findAllByOrderByConfigKeyAsc();
}
