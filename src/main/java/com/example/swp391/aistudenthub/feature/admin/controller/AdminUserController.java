package com.example.swp391.aistudenthub.feature.admin.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.request.UpdateUserStatusRequest;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminUserResponse;
import com.example.swp391.aistudenthub.feature.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin API — Quản lý người dùng.
 * Tất cả endpoint yêu cầu role ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - User Management", description = "Quản lý người dùng hệ thống (chỉ dành cho Admin)")
public class AdminUserController {

    private final AdminService adminService;

    /**
     * GET /api/v1/admin/users
     * Xem danh sách tất cả users, hỗ trợ tìm kiếm theo email/fullName và phân trang.
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả users",
               description = "Trả về danh sách users phân trang. Hỗ trợ tìm kiếm theo email hoặc tên.")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AdminUserResponse> result = adminService.getAllUsers(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/v1/admin/users/{id}
     * Xem chi tiết profile bất kỳ user nào.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết user theo ID")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserById(@PathVariable UUID id) {
        AdminUserResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * PATCH /api/v1/admin/users/{id}/status
     * Kích hoạt hoặc vô hiệu hóa tài khoản user (active = true/false).
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái kích hoạt tài khoản",
               description = "Truyền active=true để kích hoạt, active=false để vô hiệu hóa.")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        AdminUserResponse updated = adminService.updateUserStatus(id, request);
        String msg = Boolean.TRUE.equals(request.getActive())
                ? "Tài khoản đã được kích hoạt"
                : "Tài khoản đã bị vô hiệu hóa";
        return ResponseEntity.ok(ApiResponse.success(updated, msg));
    }

    /**
     * DELETE /api/v1/admin/users/{id}
     * Xóa mềm tài khoản user (set deletedAt = now, active = false).
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm tài khoản user",
               description = "Đặt deletedAt và vô hiệu hóa tài khoản. Dữ liệu không bị mất vật lý.")
    public ResponseEntity<ApiResponse<MessageResponse>> softDeleteUser(@PathVariable UUID id) {
        MessageResponse result = adminService.softDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
