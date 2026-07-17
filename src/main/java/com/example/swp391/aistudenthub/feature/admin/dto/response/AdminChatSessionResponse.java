package com.example.swp391.aistudenthub.feature.admin.dto.response;

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
public class AdminChatSessionResponse {
    private UUID sessionId;
    private String title;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private long messageCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
