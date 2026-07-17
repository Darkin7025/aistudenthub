package com.example.swp391.aistudenthub.feature.admin.service;

import com.example.swp391.aistudenthub.common.dto.PageResponse;
import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminChatMessageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminChatSessionResponse;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import com.example.swp391.aistudenthub.feature.admin.service.SystemLogService;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatMessage;
import com.example.swp391.aistudenthub.feature.chat.entity.ChatSession;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatMessageRepository;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminChatServiceImpl implements AdminChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;

    @Override
    public PageResponse<AdminChatSessionResponse> searchChatSessions(String keyword, int page, int size) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<ChatSession> sessions;
        if (keyword != null && !keyword.trim().isEmpty()) {
            sessions = chatSessionRepository.searchSessions(keyword, pageable);
        } else {
            sessions = chatSessionRepository.findAllByOrderByUpdatedAtDesc(pageable);
        }

        List<UUID> sessionUserIds = sessions.getContent().stream().map(ChatSession::getUserId).toList();
        List<UUID> sessionIds = sessions.getContent().stream().map(ChatSession::getId).toList();
        Map<UUID, User> usersById = userRepository.findAllById(sessionUserIds)
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, Long> messageCounts = sessionIds.isEmpty() ? Map.of() : chatMessageRepository.countBySessionIds(sessionIds)
                .stream().collect(Collectors.toMap(row -> (UUID) row[0], row -> ((Number) row[1]).longValue()));

        List<AdminChatSessionResponse> content = sessions.getContent().stream()
                .map(session -> mapToAdminResponse(session, usersById.get(session.getUserId()),
                        messageCounts.getOrDefault(session.getId(), 0L)))
                .collect(Collectors.toList());

        return PageResponse.<AdminChatSessionResponse>builder()
                .currentPage(page)
                .totalPages(sessions.getTotalPages())
                .pageSize(size)
                .totalElements(sessions.getTotalElements())
                .data(content)
                .build();
    }

    @Override
    public List<AdminChatMessageResponse> getMessagesBySession(UUID sessionId) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new AppException(ErrorCode.CHAT_SESSION_NOT_FOUND);
        }
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(msg -> AdminChatMessageResponse.builder()
                        .id(msg.getId())
                        .role(msg.getSender().name())
                        .content(msg.getMessage())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(UUID sessionId, UUID adminUserId, String adminEmail, String reason) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_SESSION_NOT_FOUND));
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
        systemLogService.audit("CHAT_SESSION_DELETED", "Session removed for moderation. Reason: " + reason.trim(),
                "AdminChatService", adminUserId, adminEmail, "CHAT_SESSION", sessionId.toString());
        log.info("Admin {} deleted chat session {}", adminUserId, sessionId);
    }

    private AdminChatSessionResponse mapToAdminResponse(ChatSession session, User user, long messageCount) {
        return AdminChatSessionResponse.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .userId(session.getUserId())
                .userEmail(user != null ? user.getEmail() : "Unknown")
                .userFullName(user != null ? user.getFullName() : "Unknown")
                .messageCount(messageCount)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Phân trang không hợp lệ");
        }
    }
}
