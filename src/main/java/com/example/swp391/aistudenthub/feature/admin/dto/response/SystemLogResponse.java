package com.example.swp391.aistudenthub.feature.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogResponse {
    private UUID id;
    private String level;
    private String message;
    private String stackTrace;
    private String source;
    private String action;
    private UUID actorUserId;
    private String actorEmail;
    private String targetType;
    private String targetId;
    private String requestMethod;
    private String requestPath;
    private Integer httpStatus;
    private String clientIp;
    private OffsetDateTime createdAt;
}
