package com.example.swp391.aistudenthub.feature.admin.controller;

import com.example.swp391.aistudenthub.common.dto.PageResponse;
import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminChatMessageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminChatSessionResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.request.DeleteChatSessionRequest;
import com.example.swp391.aistudenthub.feature.admin.service.AdminChatService;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/chats")
@RequiredArgsConstructor
@Tag(name = "Admin Chat Management", description = "APIs for admins to manage user chats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatController {

    private final AdminChatService adminChatService;

    @GetMapping
    @Operation(summary = "Search and view all chat sessions")
    public ResponseEntity<ApiResponse<PageResponse<AdminChatSessionResponse>>> searchChatSessions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminChatService.searchChatSessions(keyword, page, size),
                "Fetched chat sessions successfully"
        ));
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "View messages in a specific chat session")
    public ResponseEntity<ApiResponse<List<AdminChatMessageResponse>>> getSessionMessages(
            @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminChatService.getMessagesBySession(sessionId),
                "Fetched session messages successfully"
        ));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete a chat session (e.g. for violation)")
    public ResponseEntity<ApiResponse<Void>> deleteChatSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody DeleteChatSessionRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        adminChatService.deleteSession(sessionId, currentUser.getId(), currentUser.getEmail(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success(null, "Chat session deleted successfully"));
    }
}
