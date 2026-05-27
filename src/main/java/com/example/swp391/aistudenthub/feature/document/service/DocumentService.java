package com.example.swp391.aistudenthub.feature.document.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.document.dto.request.UploadDocumentRequest;
import com.example.swp391.aistudenthub.feature.document.dto.response.DocumentResponse;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Business logic cho Document Management.
 * Upload → validate → lưu Cloudinary → lưu DB → trả DTO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    // ── Giới hạn file ───────────────────────────────────────────────────────
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword", // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.ms-excel", // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.ms-powerpoint", // .ppt
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
            "text/plain", // .txt
            "image/jpeg",
            "image/png");

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final DocumentRepository documentRepository;
    private final CloudinaryService cloudinaryService;


    @Transactional
    public DocumentResponse upload(MultipartFile file,
            UploadDocumentRequest request,
            UUID userId) {
        // 1. Validate file
        validateFile(file);

        // 2. Upload lên Cloudinary
        Map<String, String> uploadResult = cloudinaryService.upload(file);

        // 3. Lưu metadata vào DB
        Document doc = Document.builder()
                .userId(userId)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .fileUrl(uploadResult.get("url"))
                .fileName(file.getOriginalFilename())
                .originalFileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .storagePublicId(uploadResult.get("public_id"))
                .storageBucket("cloudinary")
                .isPublic(true)
                .build();

        Document saved = documentRepository.save(doc);
        log.info("Document saved: id={}, user={}", saved.getId(), userId);

        return toResponse(saved);
    }

    /**
     * Lấy danh sách tài liệu của user hiện tại (chưa xóa).
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(UUID userId) {
        return documentRepository
                .findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy chi tiết 1 tài liệu theo id.
     * Nếu tài liệu private → chỉ chủ sở hữu mới xem được.
     */
    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID documentId, UUID requesterId) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!doc.isPublic() && !doc.getUserId().equals(requesterId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }
        return toResponse(doc);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .fileUrl(doc.getFileUrl())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .isPublic(doc.isPublic())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
