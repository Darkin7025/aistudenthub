package com.example.swp391.aistudenthub.feature.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DocumentChatQuotaResponse {
    private UUID documentId;
    private String documentTitle;
    private boolean isOwner;
    private String tierName;
    private int limit;
    private long used;
    private long remaining;
}
