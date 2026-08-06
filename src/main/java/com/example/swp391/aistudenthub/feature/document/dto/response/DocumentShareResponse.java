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
public class DocumentShareResponse {
    private UUID id;
    private UUID documentId;
    private UUID sharedByUserId;
    private UUID sharedWithUserId;
    private String sharedWithUserEmail;
    private String sharedWithUserName;
    private String permission;
    private OffsetDateTime createdAt;
}
