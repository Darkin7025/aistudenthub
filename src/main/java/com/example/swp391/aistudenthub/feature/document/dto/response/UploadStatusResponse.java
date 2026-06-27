package com.example.swp391.aistudenthub.feature.document.dto.response;

import com.example.swp391.aistudenthub.feature.document.enums.UploadStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UploadStatusResponse {
    private UUID documentId;
    private UploadStatus uploadStatus;
    private Integer uploadProgress;
    private String message;
}
