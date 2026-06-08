package com.example.swp391.aistudenthub.feature.chat.service;

import com.example.swp391.aistudenthub.feature.chat.dto.request.ChatRequest;
import com.example.swp391.aistudenthub.feature.chat.dto.request.DocumentChatRequest;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatMessageResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatSessionResponse;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    ChatResponse chat(ChatRequest request, UUID userId);
    ChatResponse chatWithDocument(UUID documentId, DocumentChatRequest request, UUID userId);
    org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChat(ChatRequest request, UUID userId);
    org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChatWithDocument(UUID documentId, DocumentChatRequest request, UUID userId);
    List<ChatSessionResponse> getUserSessions(UUID userId);
    List<ChatMessageResponse> getSessionMessages(UUID sessionId, UUID userId);
    void deleteSession(UUID sessionId, UUID userId);
}
