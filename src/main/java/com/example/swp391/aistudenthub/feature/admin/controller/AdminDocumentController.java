package com.example.swp391.aistudenthub.feature.admin.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminDocumentResponse;
import com.example.swp391.aistudenthub.feature.admin.service.AdminService;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.document.dto.response.UploadStatusResponse;
import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin API — Quản lý tài liệu hệ thống.
 * Tất cả endpoint yêu cầu role ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Document Management", description = "Quản lý tài liệu toàn hệ thống (chỉ dành cho Admin)")
public class AdminDocumentController {

    private final AdminService adminService;

    /**
     * GET /api/v1/admin/documents
     * Tìm kiếm và lọc tất cả documents trong hệ thống của mọi user.
     */
    @GetMapping
    @Operation(
        summary = "Xem danh sách tất cả tài liệu của mọi user",
        description = "Hỗ trợ lọc theo userId, keyword, subject, major, visibility và phân trang."
    )
    public ResponseEntity<ApiResponse<Page<AdminDocumentResponse>>> getAllDocuments(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) DocumentVisibility visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AdminDocumentResponse> result = adminService.getAllDocuments(userId, keyword, subject, major, visibility, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/v1/admin/documents/{id}
     * Xem thông tin chi tiết của bất kỳ tài liệu nào.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết tài liệu theo ID (kèm thông tin uploader)")
    public ResponseEntity<ApiResponse<AdminDocumentResponse>> getDocumentById(@PathVariable UUID id) {
        AdminDocumentResponse doc = adminService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success(doc));
    }

    /**
     * GET /api/v1/admin/documents/{id}/upload-status
     * Xem trạng thái upload của bất kỳ document nào (Admin được bypass ownership).
     */
    @GetMapping("/{id}/upload-status")
    @Operation(summary = "Xem upload status của bất kỳ tài liệu nào")
    public ResponseEntity<ApiResponse<UploadStatusResponse>> getUploadStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        UploadStatusResponse status = adminService.getDocumentUploadStatus(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * DELETE /api/v1/admin/documents/{id}
     * Xóa mềm tài liệu vi phạm của bất kỳ user nào.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Xóa tài liệu vi phạm của bất kỳ user nào",
        description = "Đặt deletedAt = now cho tài liệu và ghi log kiểm duyệt của Admin."
    )
    public ResponseEntity<ApiResponse<MessageResponse>> softDeleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        MessageResponse result = adminService.softDeleteDocumentByAdmin(id, currentUser.getId(), currentUser.getEmail());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
