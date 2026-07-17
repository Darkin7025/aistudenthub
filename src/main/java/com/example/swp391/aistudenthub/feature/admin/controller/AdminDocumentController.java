package com.example.swp391.aistudenthub.feature.admin.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.feature.admin.service.AdminService;
import com.example.swp391.aistudenthub.feature.document.dto.response.DocumentResponse;
import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import com.example.swp391.aistudenthub.feature.document.service.DocumentService;
import com.example.swp391.aistudenthub.feature.document.dto.response.UploadStatusResponse;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
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
 * Admin API — Quản lý tài liệu.
 * Tất cả endpoint yêu cầu role ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Document Management", description = "Quản lý tài liệu hệ thống (chỉ dành cho Admin)")
public class AdminDocumentController {

    private final AdminService adminService;
    private final DocumentService documentService;

    /**
     * GET /api/v1/admin/documents
     * Xem tất cả documents của mọi user, hỗ trợ tìm kiếm và phân trang.
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả tài liệu của mọi user",
               description = "Trả về danh sách tài liệu phân trang. Hỗ trợ tìm kiếm theo keyword (tiêu đề/mô tả/tên file), môn học, chuyên ngành và trạng thái hiển thị.")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getAllDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) DocumentVisibility visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentResponse> result = adminService.getAllDocuments(keyword, subject, major, visibility, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * DELETE /api/v1/admin/documents/{id}
     * Xóa tài liệu vi phạm của bất kỳ user nào.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tài liệu vi phạm của bất kỳ user nào",
               description = "Thực hiện xóa mềm tài liệu vi phạm (đặt deletedAt và ẩn tài liệu).")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteDocument(@PathVariable UUID id) {
        MessageResponse result = adminService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/v1/admin/documents/{id}/upload-status
     * Xem upload status của bất kỳ document nào.
     */
    @GetMapping("/{id}/upload-status")
    @Operation(summary = "Xem trạng thái tải lên của bất kỳ tài liệu nào",
               description = "Bypass kiểm tra quyền sở hữu và trả về trạng thái upload của tài liệu.")
    public ResponseEntity<ApiResponse<UploadStatusResponse>> getUploadStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        UploadStatusResponse status = documentService.getUploadStatus(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
