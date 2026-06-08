package com.example.swp391.aistudenthub.feature.document.dto.response;

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
public class DocumentResponse {

    private UUID id;
    private String title;
    private String description;


    private String fileUrl;
    private String fileName;

    private Long fileSize;

    private String fileType;

    private String previewMode;

    private Boolean aiSupported;

    private com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility visibility;

    private String subject;
    
    private String major;
    
    private String documentType;
    
    private com.example.swp391.aistudenthub.feature.document.enums.UploadStatus uploadStatus;
    
    private Integer uploadProgress;
    
    private UUID folderId;

    private OffsetDateTime createdAt;
    
    private String customMetadata;
}
