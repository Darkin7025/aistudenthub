package com.example.swp391.aistudenthub.feature.document.mapper;

import com.example.swp391.aistudenthub.feature.document.dto.response.DocumentResponse;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.enums.PreviewMode;
import com.example.swp391.aistudenthub.feature.document.service.DocumentPreviewResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DocumentMapper {

    private final DocumentPreviewResolver previewResolver;

    public DocumentResponse toResponse(Document doc) {
        if (doc == null) {
            return null;
        }

        PreviewMode previewMode = previewResolver.resolveMode(doc.getOriginalFileName(), doc.getFileType());
        boolean aiSupported = previewResolver.isAiCapable(previewMode) && StringUtils.hasText(doc.getExtractedText());

        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .fileUrl(doc.getFileUrl())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .previewMode(previewMode.name())
                .aiSupported(aiSupported)
                .visibility(doc.getVisibility())
                .subject(doc.getSubject())
                .major(doc.getMajor())
                .documentType(doc.getDocumentType())
                .folderId(doc.getFolderId())
                .uploadStatus(doc.getUploadStatus())
                .uploadProgress(doc.getUploadProgress())
                .createdAt(doc.getCreatedAt())
                .customMetadata(doc.getCustomMetadata())
                .extractedText(doc.getExtractedText())
                .build();
    }
}
