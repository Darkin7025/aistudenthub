package com.example.swp391.aistudenthub.feature.document.dto.request;

import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentUpdateRequest {
    @NotBlank(message = "Title cannot be blank")
    private String title;
    
    private String description;
    private String subject;
    private String major;
    private String documentType;
    private DocumentVisibility visibility;
    private java.util.UUID folderId;
    private String customMetadata;
}
