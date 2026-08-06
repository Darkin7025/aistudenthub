package com.example.swp391.aistudenthub.feature.chat.mapper;

import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatMessageResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatSessionResponse;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatMessage;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatSession;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMapper {

    private final DocumentRepository documentRepository;

    public ChatSessionResponse toSessionResponse(ChatSession session) {
        if (session == null) {
            return null;
        }

        String documentTitle = null;
        if (session.getDocumentId() != null) {
            documentTitle = documentRepository.findByIdAndDeletedAtIsNull(session.getDocumentId())
                    .map(doc -> doc.getTitle())
                    .orElse(null);
        }

        return ChatSessionResponse.builder()
                .id(session.getId())
                .documentId(session.getDocumentId())
                .documentTitle(documentTitle)
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

