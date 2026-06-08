package com.example.swp391.aistudenthub.feature.document.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.document.dto.request.UploadDocumentRequest;
import com.example.swp391.aistudenthub.feature.document.dto.response.DocumentResponse;
import com.example.swp391.aistudenthub.feature.document.service.DocumentService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho Document Management.
 *
 * <pre>
 * POST   /api/v1/documents/upload        Upload tài liệu mới
 * GET    /api/v1/documents/my            Lấy danh sách tài liệu của tôi
 * GET    /api/v1/documents/{id}          Lấy chi tiết 1 tài liệu
 * </pre>
 *
 * Tất cả endpoint đều yêu cầu JWT (anyRequest().authenticated() trong
 * SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // ── 1. UPLOAD ────────────────────────────────────────────────────────────

    /**
     * Upload tài liệu mới.
     *
     * <p>
     * Request: multipart/form-data với các field:
     * <ul>
     * <li>{@code file} — file thực (PDF, DOCX, TXT, ...)</li>
     * <li>{@code title} — tiêu đề tài liệu (bắt buộc)</li>
     * <li>{@code description} — mô tả (tuỳ chọn)</li>
     * </ul>
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") UploadDocumentRequest request,
            @AuthenticationPrincipal User currentUser) {

        DocumentResponse response = documentService.upload(file, request, currentUser.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tài liệu đã được upload thành công"));
    }

    // ── 2. GET MY DOCUMENTS ───────────────────────────────────────────────────

    /**
     * Lấy danh sách tất cả tài liệu của user đang đăng nhập.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getMyDocuments(
            @AuthenticationPrincipal User currentUser) {

        List<DocumentResponse> docs = documentService.getMyDocuments(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    // ── 3. GET BY ID ──────────────────────────────────────────────────────────

    /**
     * Lấy chi tiết 1 tài liệu.
     * Nếu tài liệu private → chỉ chủ sở hữu mới xem được (403 nếu không phải).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        DocumentResponse doc = documentService.getById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(doc));
    }

    // ── 4. DELETE ────────────────────────────────────────────────────────────

    /**
     * Xóa mềm 1 tài liệu.
     * Chỉ chủ sở hữu tài liệu mới có quyền xóa.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        MessageResponse result = documentService.deleteById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── 5. NEW ENDPOINTS FOR SU26 TASKS ──────────────────────────────────────

    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<String>> downloadDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        String downloadUrl = documentService.downloadDocument(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(downloadUrl, "Lấy link download thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocumentInfo(
            @PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody com.example.swp391.aistudenthub.feature.document.dto.request.DocumentUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        DocumentResponse updated = documentService.updateDocumentInfo(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật tài liệu thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<DocumentResponse>>> searchAndFilterDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) UUID folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        
        if (keyword != null && keyword.trim().isEmpty()) keyword = null;
        if (subject != null && subject.trim().isEmpty()) subject = null;
        if (major != null && major.trim().isEmpty()) major = null;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<DocumentResponse> result = documentService.searchAndFilterDocuments(currentUser.getId(), keyword, subject, major, folderId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<com.example.swp391.aistudenthub.feature.document.dto.response.DocumentFilterOptionsResponse>> getFilterOptions(
            @AuthenticationPrincipal User currentUser) {
        com.example.swp391.aistudenthub.feature.document.dto.response.DocumentFilterOptionsResponse result = documentService.getFilterOptions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/upload-status")
    public ResponseEntity<ApiResponse<com.example.swp391.aistudenthub.feature.document.dto.response.UploadStatusResponse>> getUploadStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        com.example.swp391.aistudenthub.feature.document.dto.response.UploadStatusResponse status = documentService.getUploadStatus(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<com.example.swp391.aistudenthub.feature.document.dto.response.PreviewResponse>> getPreview(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        com.example.swp391.aistudenthub.feature.document.dto.response.PreviewResponse preview = documentService.getPreview(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<byte[]> streamDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return documentService.streamDocument(id, currentUser);
    }
}
