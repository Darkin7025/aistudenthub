package com.example.swp391.aistudenthub.feature.admin.repository;

import com.example.swp391.aistudenthub.feature.admin.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
}
