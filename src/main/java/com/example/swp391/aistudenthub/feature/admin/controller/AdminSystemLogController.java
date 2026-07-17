package com.example.swp391.aistudenthub.feature.admin.controller;

import com.example.swp391.aistudenthub.common.dto.PageResponse;
import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.SystemLogResponse;
import com.example.swp391.aistudenthub.feature.admin.entity.LogLevel;
import com.example.swp391.aistudenthub.feature.admin.service.SystemLogService;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@Tag(name = "Admin System Logs", description = "APIs for viewing and managing system logs")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminSystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    @Operation(summary = "Get system logs with optional level filtering")
    public ResponseEntity<ApiResponse<PageResponse<SystemLogResponse>>> getLogs(
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        if (page < 1 || size > 100 || (from != null && to != null && from.isAfter(to))) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Phân trang hoặc khoảng thời gian không hợp lệ");
        }
        return ResponseEntity.ok(ApiResponse.success(
                systemLogService.getLogs(level, action, actorUserId, source, from, to, page, size),
                "Fetched system logs successfully"
        ));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear logs older than a specific date")
    public ResponseEntity<ApiResponse<Void>> clearLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime before,
            @AuthenticationPrincipal User currentUser
    ) {
        systemLogService.clearLogsBefore(before, currentUser.getId(), currentUser.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "System logs cleared successfully"));
    }
}
