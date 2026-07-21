package com.example.swp391.aistudenthub.feature.admin.service;

import com.example.swp391.aistudenthub.common.dto.PageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.SystemLogResponse;
import com.example.swp391.aistudenthub.feature.admin.entity.LogLevel;
import com.example.swp391.aistudenthub.feature.admin.entity.SystemLog;
import com.example.swp391.aistudenthub.feature.admin.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl implements SystemLogService {

    private final SystemLogRepository systemLogRepository;

    @Override
    @Transactional
    public void log(LogLevel level, String message, String stackTrace, String source) {
        save(SystemLog.builder()
                .level(level)
                .message(message)
                .stackTrace(stackTrace)
                .source(source)
                .build());
    }

    @Override
    @Transactional
    public void audit(String action, String message, String source, UUID actorUserId, String actorEmail,
                      String targetType, String targetId) {
        save(SystemLog.builder()
                .level(LogLevel.INFO)
                .action(action)
                .message(message)
                .source(source)
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .targetType(targetType)
                .targetId(targetId)
                .build());
    }

    @Override
    @Transactional
    public void logError(String message, String stackTrace, String source, String requestMethod,
                         String requestPath, Integer httpStatus, String clientIp,
                         UUID actorUserId, String actorEmail) {
        save(SystemLog.builder()
                .level(LogLevel.ERROR)
                .message(message)
                .stackTrace(stackTrace)
                .source(source)
                .requestMethod(requestMethod)
                .requestPath(requestPath)
                .httpStatus(httpStatus)
                .clientIp(clientIp)
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .build());
    }

    private void save(SystemLog systemLog) {
        try {
            systemLogRepository.save(systemLog);
        } catch (Exception e) {
            log.error("Failed to save system log to DB", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SystemLogResponse> getLogs(LogLevel level, String action, UUID actorUserId,
                                                   String source, OffsetDateTime from, OffsetDateTime to,
                                                   int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        String normAction = normalize(action);
        String normSource = normalize(source);

        Page<SystemLog> logs;
        if (level == null && normAction == null && actorUserId == null && normSource == null && from == null && to == null) {
            logs = systemLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            logs = systemLogRepository.searchLogs(level, normAction, actorUserId, normSource, from, to, pageable);
        }

        List<SystemLogResponse> content = logs.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<SystemLogResponse>builder()
                .currentPage(page)
                .totalPages(logs.getTotalPages())
                .pageSize(size)
                .totalElements(logs.getTotalElements())
                .data(content)
                .build();
    }

    @Override
    @Transactional
    public void clearLogsBefore(OffsetDateTime date, UUID adminUserId, String adminEmail) {
        systemLogRepository.deleteByCreatedAtBefore(date);
        audit("SYSTEM_LOGS_PURGED", "System logs purged before " + date, "SystemLogService",
                adminUserId, adminEmail, "SYSTEM_LOG", null);
        log.info("Cleared system logs before {}", date);
    }

    private SystemLogResponse mapToResponse(SystemLog log) {
        return SystemLogResponse.builder()
                .id(log.getId())
                .level(log.getLevel().name())
                .message(log.getMessage())
                .stackTrace(log.getStackTrace())
                .source(log.getSource())
                .action(log.getAction())
                .actorUserId(log.getActorUserId())
                .actorEmail(log.getActorEmail())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .requestMethod(log.getRequestMethod())
                .requestPath(log.getRequestPath())
                .httpStatus(log.getHttpStatus())
                .clientIp(log.getClientIp())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
