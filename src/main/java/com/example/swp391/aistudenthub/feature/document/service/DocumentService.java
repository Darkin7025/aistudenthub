package com.example.swp391.aistudenthub.feature.document.service;

import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.document.dto.request.UploadDocumentRequest;
import com.example.swp391.aistudenthub.feature.document.dto.response.DocumentResponse;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.entity.Folder;
import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import com.example.swp391.aistudenthub.feature.document.enums.PreviewMode;
import com.example.swp391.aistudenthub.feature.document.mapper.DocumentMapper;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import com.example.swp391.aistudenthub.feature.document.repository.FolderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final int TEXT_PREVIEW_LIMIT = 50_000;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final String OFFICE_PREVIEW_MESSAGE = "Office preview uses external viewer. If it cannot load, please download the file.";
    private static final String UNSUPPORTED_PREVIEW_MESSAGE = "Preview is not supported for this file type. Please download the file.";
    private static final String MISSING_FILE_URL_MESSAGE = "Preview is unavailable because the file URL is missing. Please download the file.";

    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;
    private final CloudinaryService cloudinaryService;
    private final DocumentMapper documentMapper;
    private final ObjectMapper objectMapper;
    private final DocumentPreviewResolver previewResolver;

    @Transactional
    public DocumentResponse upload(MultipartFile file, UploadDocumentRequest request, UUID userId) {
        validateFile(file);
        validateCustomMetadata(request.getCustomMetadata());
        validateFolderOwnership(request.getFolderId(), userId);

        String extractedText = null;
        PreviewMode previewMode = previewResolver.resolveMode(file.getOriginalFilename(), file.getContentType());

        try {
            if (PreviewMode.TEXT.equals(previewMode)) {
                extractedText = new String(file.getBytes(), StandardCharsets.UTF_8);
                if (extractedText.trim().isEmpty()) {
                    log.warn("Text file is empty: {}", file.getOriginalFilename());
                    extractedText = null;
                } else {
                    log.info("Extracted {} chars from text file: {}", extractedText.length(),
                            file.getOriginalFilename());
                }
            } else if (PreviewMode.PDF.equals(previewMode)) {
                try (PDDocument pdDoc = Loader.loadPDF(file.getBytes())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    extractedText = stripper.getText(pdDoc);

                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        log.info("Extracted {} chars from {} pages PDF: {}",
                                extractedText.length(), pdDoc.getNumberOfPages(), file.getOriginalFilename());
                    } else {
                        log.warn("PDF has no extractable text (might be scanned image): {}",
                                file.getOriginalFilename());
                        extractedText = null;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract text from file {}. Error: {}", file.getOriginalFilename(), e.getMessage(), e);
            extractedText = null;
        }

        Map<String, String> uploadResult = cloudinaryService.upload(file);

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
                .storageKey(uploadResult.get("public_id"))
                .storageResourceType(uploadResult.get("resource_type"))
                .storageBucket("cloudinary")
                .visibility(request.getVisibility() != null ? request.getVisibility() : DocumentVisibility.PUBLIC)
                .subject(request.getSubject())
                .major(request.getMajor())
                .documentType(request.getDocumentType())
                .folderId(request.getFolderId())
                .customMetadata(request.getCustomMetadata())
                .extractedText(extractedText)
                .build();

        Document saved = documentRepository.save(doc);
        log.info("Document saved: id={}, user={}", saved.getId(), userId);
        return documentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(UUID userId) {
        return documentRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID documentId, UUID requesterId) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!doc.getUserId().equals(requesterId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }
        return documentMapper.toResponse(doc);
    }

    @Transactional
    public void delete(UUID documentId, UUID userId) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!doc.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        doc.setDeletedAt(java.time.OffsetDateTime.now());
        documentRepository.save(doc);
        log.info("Document soft-deleted: id={}, user={}", documentId, userId);
    }

    @Transactional
    public MessageResponse deleteById(UUID documentId, UUID userId) {
        delete(documentId, userId);
        return new MessageResponse("Tài liệu đã được xóa thành công");
    }

    @Transactional(readOnly = true)
    public String downloadDocument(UUID documentId, UUID requesterId) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!doc.getUserId().equals(requesterId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return doc.getFileUrl();
    }

    @Transactional
    public DocumentResponse updateDocumentInfo(
            UUID documentId,
            com.example.swp391.aistudenthub.feature.document.dto.request.DocumentUpdateRequest request,
            UUID requesterId) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!doc.getUserId().equals(requesterId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        validateCustomMetadata(request.getCustomMetadata());
        validateFolderOwnership(request.getFolderId(), requesterId);

        doc.setTitle(request.getTitle().trim());
        doc.setDescription(request.getDescription());
        doc.setSubject(request.getSubject());
        doc.setMajor(request.getMajor());
        doc.setDocumentType(request.getDocumentType());
        doc.setFolderId(request.getFolderId());
        doc.setCustomMetadata(request.getCustomMetadata());

        if (request.getVisibility() != null) {
            doc.setVisibility(request.getVisibility());
        }

        Document saved = documentRepository.save(doc);
        return documentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DocumentResponse> searchAndFilterDocuments(
            UUID userId,
            String keyword,
            String subject,
            String major,
            UUID folderId,
            org.springframework.data.domain.Pageable pageable) {
        return documentRepository.searchAndFilter(userId, null, keyword, subject, major, folderId, pageable)
                .map(documentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public com.example.swp391.aistudenthub.feature.document.dto.response.DocumentFilterOptionsResponse getFilterOptions(
            UUID userId) {
        List<String> subjects = documentRepository.findDistinctSubjectsByUserId(userId);
        List<String> majors = documentRepository.findDistinctMajorsByUserId(userId);
        return com.example.swp391.aistudenthub.feature.document.dto.response.DocumentFilterOptionsResponse.builder()
                .subjects(subjects)
                .majors(majors)
                .build();
    }

    @Transactional(readOnly = true)
    public com.example.swp391.aistudenthub.feature.document.dto.response.UploadStatusResponse getUploadStatus(
            UUID documentId,
            com.example.swp391.aistudenthub.feature.auth.entity.User currentUser) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!doc.getUserId().equals(currentUser.getId())
                && !com.example.swp391.aistudenthub.feature.auth.entity.Role.ADMIN.equals(currentUser.getRole())) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return com.example.swp391.aistudenthub.feature.document.dto.response.UploadStatusResponse.builder()
                .documentId(doc.getId())
                .uploadStatus(doc.getUploadStatus())
                .uploadProgress(doc.getUploadProgress())
                .message("Upload is " + doc.getUploadStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public com.example.swp391.aistudenthub.feature.document.dto.response.PreviewResponse getPreview(
            UUID documentId,
            com.example.swp391.aistudenthub.feature.auth.entity.User currentUser) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!canPreviewDocument(doc, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        String fileName = StringUtils.hasText(doc.getOriginalFileName()) ? doc.getOriginalFileName()
                : doc.getFileName();

        String previewUrl = doc.getFileUrl();
        if (doc.getStoragePublicId() != null && "cloudinary".equals(doc.getStorageBucket())) {
            String format = null;
            if (doc.getFileUrl() != null && doc.getFileUrl().endsWith(".pdf")
                    && !doc.getStoragePublicId().toLowerCase().endsWith(".pdf")) {
                format = "pdf";
            }

            String signedUrl = cloudinaryService.getSignedUrl(
                    doc.getStoragePublicId(),
                    resolveResourceType(doc),
                    format);
            if (signedUrl != null) {
                previewUrl = signedUrl;
            }
        }

        PreviewMode previewMode = previewResolver.resolveMode(fileName, doc.getFileType());
        boolean aiSupported = previewResolver.isAiCapable(previewMode) && StringUtils.hasText(doc.getExtractedText());
        boolean truncated = false;
        boolean previewSupported;
        String message = null;
        String textContent = null;

        if (PreviewMode.TEXT.equals(previewMode)) {
            previewSupported = true;
            textContent = doc.getExtractedText();
            if (textContent != null && textContent.length() > TEXT_PREVIEW_LIMIT) {
                textContent = textContent.substring(0, TEXT_PREVIEW_LIMIT);
                truncated = true;
            }
            if (!StringUtils.hasText(textContent)) {
                textContent = "";
                message = "Không có nội dung văn bản để hiển thị.";
            }
        } else if (PreviewMode.PDF.equals(previewMode)) {
            if (!StringUtils.hasText(previewUrl)) {
                if (StringUtils.hasText(doc.getExtractedText())) {
                    previewMode = PreviewMode.TEXT;
                    previewSupported = true;
                    textContent = doc.getExtractedText();
                    if (textContent.length() > TEXT_PREVIEW_LIMIT) {
                        textContent = textContent.substring(0, TEXT_PREVIEW_LIMIT);
                        truncated = true;
                    }
                    message = "File PDF không có URL để xem trực tiếp. Hiển thị nội dung văn bản đã trích xuất.";
                } else {
                    previewSupported = false;
                    message = "File PDF không có URL để xem trước. Vui lòng liên hệ quản trị viên.";
                }
            } else {
                previewSupported = true;
                if (StringUtils.hasText(doc.getExtractedText())) {
                    textContent = doc.getExtractedText();
                    if (textContent.length() > TEXT_PREVIEW_LIMIT) {
                        textContent = textContent.substring(0, TEXT_PREVIEW_LIMIT);
                        truncated = true;
                    }
                    message = "Nếu PDF không hiển thị, vui lòng tải xuống.";
                } else {
                    textContent = null;
                    message = "Nếu PDF không hiển thị, vui lòng tải xuống. (File không chứa text có thể trích xuất)";
                }
            }
        } else if (PreviewMode.UNSUPPORTED.equals(previewMode)) {
            previewSupported = false;
            message = UNSUPPORTED_PREVIEW_MESSAGE;
        } else if (!StringUtils.hasText(previewUrl)) {
            previewSupported = false;
            message = MISSING_FILE_URL_MESSAGE;
        } else {
            previewSupported = true;
            if (PreviewMode.OFFICE.equals(previewMode)) {
                message = OFFICE_PREVIEW_MESSAGE;
            }
        }

        return com.example.swp391.aistudenthub.feature.document.dto.response.PreviewResponse.builder()
                .documentId(doc.getId())
                .fileName(fileName)
                .fileType(doc.getFileType())
                .previewUrl(previewUrl)
                .previewSupported(previewSupported)
                .previewMode(previewMode.name())
                .textContent(textContent)
                .truncated(truncated)
                .aiSupported(aiSupported)
                .message(message)
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.EMPTY_FILE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    private void validateCustomMetadata(String customMetadata) {
        if (customMetadata == null || customMetadata.trim().isEmpty()) {
            return;
        }
        try {
            List<Map<String, String>> metadataList = objectMapper.readValue(
                    customMetadata,
                    new TypeReference<List<Map<String, String>>>() {
                    });
            if (metadataList != null) {
                for (Map<String, String> item : metadataList) {
                    String key = item.get("key");
                    String value = item.get("value");
                    if (key != null && key.length() > 100) {
                        throw new IllegalArgumentException("Metadata key quá dài (tối đa 100 ký tự)");
                    }
                    if (value != null && value.length() > 1000) {
                        throw new IllegalArgumentException("Metadata value quá dài (tối đa 1000 ký tự)");
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        } catch (Exception e) {
            // Invalid JSON (e.g. Swagger/Postman placeholder "string") — silently ignore
            log.warn("customMetadata is not valid JSON, skipping validation: {}", e.getMessage());
        }
    }

    private void validateFolderOwnership(UUID folderId, UUID userId) {
        if (folderId == null) {
            return;
        }

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR));

        if (!folder.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private boolean canPreviewDocument(
            Document doc,
            com.example.swp391.aistudenthub.feature.auth.entity.User currentUser) {
        return doc.getUserId().equals(currentUser.getId())
                || com.example.swp391.aistudenthub.feature.auth.entity.Role.ADMIN.equals(currentUser.getRole());
    }

    /**
     * Determines the correct Cloudinary resource_type for signed URL generation.
     * Legacy PDFs were uploaded as "raw", new PDFs are uploaded as "image".
     * Falls back to the stored value, then derives from MIME type, then defaults to "image".
     * PDFs are uploaded as "raw" — must use "raw" when building the signed URL too.
     * Falls back to the stored value, then derives from MIME type, then defaults to
     * "image".
     */
    private String resolveResourceType(Document doc) {
        if (doc.getStorageResourceType() != null) {
            return doc.getStorageResourceType();
        }
        // Derive from MIME type for documents without stored resourceType (legacy rows)
        String mime = doc.getFileType();
        if ("application/pdf".equalsIgnoreCase(mime)) {
            return "raw";
        }
        String fileName = doc.getOriginalFileName() != null ? doc.getOriginalFileName() : doc.getFileName();
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            return "raw";
        }
        return "image";
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> streamDocument(UUID documentId,
            com.example.swp391.aistudenthub.feature.auth.entity.User currentUser) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!canPreviewDocument(doc, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN_ACCESS);
        }

        try {
            String targetUrl = doc.getFileUrl();
            if (doc.getStoragePublicId() != null && "cloudinary".equals(doc.getStorageBucket())) {
                String format = null;
                if (doc.getFileUrl() != null && doc.getFileUrl().endsWith(".pdf")
                        && !doc.getStoragePublicId().toLowerCase().endsWith(".pdf")) {
                    format = "pdf";
                }

                String signedUrl = cloudinaryService.getSignedUrl(
                        doc.getStoragePublicId(),
                        resolveResourceType(doc),
                        format);
                if (signedUrl != null) {
                    targetUrl = signedUrl;
                }
            }

            URL url = new URL(targetUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            if (responseCode >= 400) {
                String errorMsg = "";
                try (InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        errorMsg = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                log.error("Cloudinary returned HTTP {} for URL {}. Error details: {}", responseCode, targetUrl,
                        errorMsg);
                throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
            }

            try (InputStream in = conn.getInputStream()) {
                byte[] content = in.readAllBytes();

                HttpHeaders headers = new HttpHeaders();

                if (doc.getFileType() != null) {
                    headers.setContentType(MediaType.parseMediaType(doc.getFileType()));
                } else {
                    headers.setContentType(MediaType.APPLICATION_PDF);
                }

                String fileName = doc.getOriginalFileName() != null ? doc.getOriginalFileName() : doc.getFileName();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
                headers.setCacheControl("max-age=3600");

                log.info("Streamed document {} to user {}", documentId, currentUser.getId());
                return new ResponseEntity<>(content, headers, HttpStatus.OK);
            }
        } catch (IOException e) {
            log.error("Failed to stream document {} from {}", documentId, doc.getFileUrl(), e);
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
    }
}
