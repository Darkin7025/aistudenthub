package com.example.swp391.aistudenthub.feature.admin.repository;

import com.example.swp391.aistudenthub.feature.admin.entity.LogLevel;
import com.example.swp391.aistudenthub.feature.admin.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, UUID> {
    boolean existsByAction(String action);

    Page<SystemLog> findByLevelOrderByCreatedAtDesc(LogLevel level, Pageable pageable);
    Page<SystemLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT s FROM SystemLog s WHERE " +
            "(:level IS NULL OR s.level = :level) AND " +
            "(:action IS NULL OR LOWER(s.action) LIKE LOWER(CONCAT('%', :action, '%'))) AND " +
            "(:actorUserId IS NULL OR s.actorUserId = :actorUserId) AND " +
            "(:source IS NULL OR LOWER(s.source) LIKE LOWER(CONCAT('%', :source, '%'))) AND " +
            "(:from IS NULL OR s.createdAt >= :from) AND " +
            "(:to IS NULL OR s.createdAt <= :to) " +
            "ORDER BY s.createdAt DESC")
    Page<SystemLog> searchLogs(@Param("level") LogLevel level,
                               @Param("action") String action,
                               @Param("actorUserId") UUID actorUserId,
                               @Param("source") String source,
                               @Param("from") OffsetDateTime from,
                               @Param("to") OffsetDateTime to,
                               Pageable pageable);

    @Modifying
    @Query("DELETE FROM SystemLog s WHERE s.createdAt < :before")
    void deleteByCreatedAtBefore(@Param("before") OffsetDateTime before);
}
