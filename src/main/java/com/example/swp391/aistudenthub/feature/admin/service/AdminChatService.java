package com.example.swp391.aistudenthub.feature.admin.service;

import com.example.swp391.aistudenthub.common.dto.PageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminChatMessageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminChatSessionResponse;

import java.util.List;
import java.util.UUID;

public interface AdminChatService {
    PageResponse<AdminChatSessionResponse> searchChatSessions(String keyword, int page, int size);
    List<AdminChatMessageResponse> getMessagesBySession(UUID sessionId);
    void deleteSession(UUID sessionId, UUID adminUserId, String adminEmail, String reason);
}
