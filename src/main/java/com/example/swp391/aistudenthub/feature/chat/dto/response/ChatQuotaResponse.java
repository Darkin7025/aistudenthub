package com.example.swp391.aistudenthub.feature.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ChatQuotaResponse {
    private int dailyLimit;
    private long used;
    private long remaining;
    private OffsetDateTime resetAt;
}
