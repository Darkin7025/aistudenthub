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
public class AdminChatMessageResponse {
    private UUID id;
    private String role;
    private String content;
    private OffsetDateTime createdAt;
}
