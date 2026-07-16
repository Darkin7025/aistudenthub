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

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final jakarta.validation.Validator validator;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @io.swagger.v3.oas.annotations.Parameter(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UploadDocumentRequest.class)) @RequestPart("request") String requestJson,
            @AuthenticationPrincipal User currentUser) {

        UploadDocumentRequest request;
        try {
            request = objectMapper.readValue(requestJson, UploadDocumentRequest.class);
        } catch (Exception e) {
            throw new com.example.swp391.aistudenthub.exception.AppException(
                    com.example.swp391.aistudenthub.exception.ErrorCode.VALIDATION_ERROR, "Dữ liệu JSON không hợp lệ");
        }

        java.util.Set<jakarta.validation.ConstraintViolation<UploadDocumentRequest>> violations = validator
                .validate(request);
        if (!violations.isEmpty()) {
            String message = violations.iterator().next().getMessage();
            throw new com.example.swp391.aistudenthub.exception.AppException(
                    com.example.swp391.aistudenthub.exception.ErrorCode.VALIDATION_ERROR,
                    message);
        }

        DocumentResponse response = documentService.upload(file, request, currentUser.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tài liệu đã được upload thành công"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getMyDocuments(
            @AuthenticationPrincipal User currentUser) {

        List<DocumentResponse> docs = documentService.getMyDocuments(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<DocumentResponse>>> searchPublicDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String major,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (keyword != null && keyword.trim().isEmpty())
            keyword = null;
        if (subject != null && subject.trim().isEmpty())
            subject = null;
        if (major != null && major.trim().isEmpty())
            major = null;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<DocumentResponse> result = documentService
                .searchPublicDocuments(keyword, subject, major, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/public/filter-options")
    public ResponseEntity<ApiResponse<com.example.swp391.aistudenthub.feature.document.dto.response.DocumentFilterOptionsResponse>> getPublicFilterOptions() {
        com.example.swp391.aistudenthub.feature.document.dto.response.DocumentFilterOptionsResponse result = documentService
                .getPublicFilterOptions();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        DocumentResponse doc = documentService.getById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(doc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        MessageResponse result = documentService.deleteById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<String>> downloadDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        String downloadUrl = documentService.downloadDocument(id, currentUser);
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

        if (keyword != null && keyword.trim().isEmpty())
            keyword = null;
        if (subject != null && subject.trim().isEmpty())
            subject = null;
        if (major != null && major.trim().isEmpty())
            major = null;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<DocumentResponse> result = documentService
                .searchAndFilterDocuments(currentUser.getId(), keyword, subject, major, folderId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<com.example.swp391.aistudenthub.feature.document.dto.response.DocumentFilterOptionsResponse>> getFilterOptions(
            @AuthenticationPrincipal User currentUser) {
        com.example.swp391.aistudenthub.feature.document.dto.response.DocumentFilterOptionsResponse result = documentService
                .getFilterOptions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/upload-status")
    public ResponseEntity<ApiResponse<com.example.swp391.aistudenthub.feature.document.dto.response.UploadStatusResponse>> getUploadStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        com.example.swp391.aistudenthub.feature.document.dto.response.UploadStatusResponse status = documentService
                .getUploadStatus(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<com.example.swp391.aistudenthub.feature.document.dto.response.PreviewResponse>> getPreview(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        com.example.swp391.aistudenthub.feature.document.dto.response.PreviewResponse preview = documentService
                .getPreview(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<byte[]> streamDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return documentService.streamDocument(id, currentUser);
    }
}
