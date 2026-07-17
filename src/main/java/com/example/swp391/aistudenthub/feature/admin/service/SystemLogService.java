package com.example.swp391.aistudenthub.feature.admin.service;

import com.example.swp391.aistudenthub.common.dto.PageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.SystemLogResponse;
import com.example.swp391.aistudenthub.feature.admin.entity.LogLevel;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SystemLogService {
    void log(LogLevel level, String message, String stackTrace, String source);
    void audit(String action, String message, String source, UUID actorUserId, String actorEmail,
               String targetType, String targetId);
    void logError(String message, String stackTrace, String source, String requestMethod,
                  String requestPath, Integer httpStatus, String clientIp, UUID actorUserId, String actorEmail);
    PageResponse<SystemLogResponse> getLogs(LogLevel level, String action, UUID actorUserId,
                                            String source, OffsetDateTime from, OffsetDateTime to,
                                            int page, int size);
    void clearLogsBefore(OffsetDateTime date, UUID adminUserId, String adminEmail);
}
