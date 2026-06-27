package com.example.swp391.aistudenthub.feature.chat.mapper;

import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatMessageResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatSessionResponse;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatMessage;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatSession;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ChatSessionResponse toSessionResponse(ChatSession session) {
        if (session == null) {
            return null;
        }

        return ChatSessionResponse.builder()
                .id(session.getId())
                .documentId(session.getDocumentId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    public ChatMessageResponse toMessageResponse(ChatMessage message) {
        if (message == null) {
            return null;
        }

        return ChatMessageResponse.builder()
                .id(message.getId())
                .sender(message.getSender())
                .message(message.getMessage())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
