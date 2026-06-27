package com.example.swp391.aistudenthub.feature.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ChatResponse {
    private String answer;
    private UUID sessionId;
    private UUID documentId; // Only present if document chat
}
