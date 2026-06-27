package com.example.swp391.aistudenthub.feature.chat.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.chat.dto.request.ChatRequest;
import com.example.swp391.aistudenthub.feature.chat.dto.request.DocumentChatRequest;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatMessageResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatResponse;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatSessionResponse;
import com.example.swp391.aistudenthub.feature.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI chatbot và hỏi đáp theo tài liệu")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Chat thường với AI")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatResponse response = chatService.chat(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/document/{documentId}")
    @Operation(summary = "Hỏi AI về nội dung một tài liệu")
    public ResponseEntity<ApiResponse<ChatResponse>> chatWithDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody DocumentChatRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatResponse response = chatService.chatWithDocument(documentId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Chat stream SSE (tuỳ chọn cho phiên bản sau)")
    public SseEmitter streamChat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal User currentUser) {
        return chatService.streamChat(request, currentUser.getId());
    }

    @PostMapping(value = "/document/{documentId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Hỏi tài liệu bằng SSE stream (tuỳ chọn cho phiên bản sau)")
    public SseEmitter streamChatWithDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody DocumentChatRequest request,
            @AuthenticationPrincipal User currentUser) {
        return chatService.streamChatWithDocument(documentId, request, currentUser.getId());
    }

    @GetMapping("/sessions")
    @Operation(summary = "Lấy lịch sử hội thoại của user hiện tại")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getUserSessions(
            @AuthenticationPrincipal User currentUser) {
        List<ChatSessionResponse> sessions = chatService.getUserSessions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Lấy tin nhắn trong một hội thoại")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getSessionMessages(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser) {
        List<ChatMessageResponse> messages = chatService.getSessionMessages(sessionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Xoá một phiên hội thoại")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser) {
        chatService.deleteSession(sessionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
