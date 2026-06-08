package com.example.swp391.aistudenthub.feature.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatSessionResponse {
    private UUID id;
    private UUID documentId;
    private String title;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
