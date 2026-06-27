package com.example.swp391.aistudenthub.feature.document.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreviewResponse {
    private java.util.UUID documentId;
    private String fileName;
    private String previewUrl;
    private String fileType;
    private boolean previewSupported;
    private String previewMode; // PDF, IMAGE, TEXT, OFFICE, UNSUPPORTED
    private String textContent;
    private boolean truncated;
    private boolean aiSupported;
    private String message;
}
