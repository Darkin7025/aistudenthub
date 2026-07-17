package com.example.swp391.aistudenthub.feature.admin.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.request.UpdateSystemConfigRequest;
import com.example.swp391.aistudenthub.feature.admin.dto.response.SystemConfigResponse;
import com.example.swp391.aistudenthub.feature.admin.service.AdminService;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin API — Cấu hình hệ thống.
 * Cho phép Admin bật/tắt tính năng hoặc điều chỉnh thông số tại runtime.
 */
@RestController
@RequestMapping("/api/v1/admin/system-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - System Config", description = "Quản lý cấu hình hệ thống tại runtime (chỉ dành cho Admin)")
public class AdminSystemConfigController {

    private final AdminService adminService;

    /**
     * GET /api/v1/admin/system-config
     * Lấy toàn bộ danh sách cấu hình hệ thống hiện tại.
     */
    @GetMapping
    @Operation(summary = "Lấy toàn bộ cấu hình hệ thống",
               description = "Trả về tất cả key-value config đang được lưu trong DB.")
    public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> getAllConfigs() {
        List<SystemConfigResponse> configs = adminService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    /**
     * PUT /api/v1/admin/system-config
     * Cập nhật hàng loạt cấu hình hệ thống (upsert theo configKey).
     * Nếu key chưa tồn tại thì tạo mới; nếu đã tồn tại thì ghi đè value.
     *
     * <p>Ví dụ request body:
     * <pre>
     * {
     *   "configs": [
     *     { "configKey": "feature.ai_chat.enabled", "configValue": "true" },
     *     { "configKey": "feature.upload.enabled",  "configValue": "false" }
     *   ]
     * }
     * </pre>
     */
    @PutMapping
    @Operation(summary = "Cập nhật cấu hình hệ thống (batch upsert)",
               description = "Upsert nhiều key-value config trong một request. Key chưa có sẽ được tạo mới.")
    public ResponseEntity<ApiResponse<List<SystemConfigResponse>>> updateConfigs(
            @Valid @RequestBody UpdateSystemConfigRequest request,
            @AuthenticationPrincipal User currentUser) {

        List<SystemConfigResponse> updated = adminService.updateConfigs(request, currentUser.getId(), currentUser.getEmail());
        return ResponseEntity.ok(ApiResponse.success(updated, "Cấu hình hệ thống đã được cập nhật thành công"));
    }
}
