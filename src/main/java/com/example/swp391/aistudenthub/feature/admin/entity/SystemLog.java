package com.example.swp391.aistudenthub.feature.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_logs", indexes = {
        @Index(name = "idx_syslog_level", columnList = "level"),
        @Index(name = "idx_syslog_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogLevel level;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(length = 255)
    private String source;

    /** A stable, searchable identifier for an audit event, e.g. CHAT_SESSION_DELETED. */
    @Column(length = 100)
    private String action;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(name = "target_id", length = 255)
    private String targetId;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_path", length = 1000)
    private String requestPath;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
