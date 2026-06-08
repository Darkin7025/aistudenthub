package com.example.swp391.aistudenthub.feature.chat.dto.response;

import com.example.swp391.aistudenthub.feature.chat.enums.MessageSender;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatMessageResponse {
    private UUID id;
    private MessageSender sender;
    private String message;
    private OffsetDateTime createdAt;
}
