package com.example.swp391.aistudenthub.feature.admin.dto.response;

import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import com.example.swp391.aistudenthub.feature.document.enums.UploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDocumentResponse {

    private UUID id;
    private UUID userId;
    private String uploaderEmail;
    private String uploaderFullName;

    private String title;
    private String description;

    private String fileUrl;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private String previewMode;
    private Boolean aiSupported;

    private DocumentVisibility visibility;
    private String subject;
    private String major;
    private String documentType;
    private UploadStatus uploadStatus;
    private Integer uploadProgress;
    private UUID folderId;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
    private String customMetadata;
}
