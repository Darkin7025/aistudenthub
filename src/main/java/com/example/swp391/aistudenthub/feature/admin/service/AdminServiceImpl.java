package com.example.swp391.aistudenthub.feature.admin.service;

import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.admin.dto.request.UpdateSystemConfigRequest;
import com.example.swp391.aistudenthub.feature.admin.dto.request.UpdateUserStatusRequest;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminDashboardStatsResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminUserResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AiUsageResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.DocumentTypeStatResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.SystemConfigResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.UploadTrendResponse;
import com.example.swp391.aistudenthub.feature.admin.entity.SystemConfig;
import com.example.swp391.aistudenthub.feature.admin.repository.SystemConfigRepository;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatSessionRepository;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import com.example.swp391.aistudenthub.feature.document.dto.response.DocumentResponse;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import com.example.swp391.aistudenthub.feature.document.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final DocumentMapper documentMapper;

    // =========================================================
    // USER MANAGEMENT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(String keyword, Pageable pageable) {
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        if (keyword != null) {
            return userRepository.searchUsers(keyword, pageable).map(this::toAdminUserResponse);
        }
        return userRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toAdminUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return toAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setActive(request.getActive());
        User saved = userRepository.save(user);
        log.info("Admin updated user {} active={}", userId, request.getActive());
        return toAdminUserResponse(saved);
    }

    @Override
    @Transactional
    public MessageResponse softDeleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tài khoản này đã bị xóa trước đó");
        }

        user.setDeletedAt(OffsetDateTime.now());
        user.setActive(false);
        userRepository.save(user);
        log.info("Admin soft-deleted user {}", userId);
        return new MessageResponse("Tài khoản người dùng đã được xóa thành công");
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        long totalUsers     = userRepository.countByDeletedAtIsNull();
        long totalDocs      = documentRepository.countByDeletedAtIsNull();
        long totalSessions  = chatSessionRepository.count();
        long disabledUsers  = userRepository.findAll().stream()
                .filter(u -> !u.isActive() && u.getDeletedAt() == null)
                .count();

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalDocuments(totalDocs)
                .totalChatSessions(totalSessions)
                .disabledUsers(disabledUsers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentTypeStatResponse> getDocumentTypeStats() {
        return documentRepository.countByFileType().stream()
                .map(row -> {
                    String mimeType = row[0] != null ? (String) row[0] : "unknown";
                    long count      = ((Number) row[1]).longValue();
                    return DocumentTypeStatResponse.builder()
                            .fileType(mimeType)
                            .label(resolveMimeLabel(mimeType))
                            .count(count)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UploadTrendResponse> getUploadTrend(int days) {
        if (days <= 0 || days > 365) days = 30;
        OffsetDateTime from = OffsetDateTime.now().minusDays(days);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return documentRepository.countUploadsByDay(from).stream()
                .map(row -> UploadTrendResponse.builder()
                        .date(row[0].toString())
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AiUsageResponse getAiUsage() {
        long totalDocs       = documentRepository.countByDeletedAtIsNull();
        long docsWithAi      = documentRepository.countDocumentsWithAiUsage();
        long docsWithout     = totalDocs - docsWithAi;
        double percent       = totalDocs == 0 ? 0.0
                : BigDecimal.valueOf(docsWithAi * 100.0 / totalDocs)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();

        return AiUsageResponse.builder()
                .totalDocuments(totalDocs)
                .documentsWithAiChat(docsWithAi)
                .documentsWithoutAiChat(docsWithout)
                .aiUsagePercent(percent)
                .build();
    }

    // =========================================================
    // SYSTEM CONFIG
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfigResponse> getAllConfigs() {
        return systemConfigRepository.findAll()
                .stream()
                .map(this::toConfigResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<SystemConfigResponse> updateConfigs(UpdateSystemConfigRequest request) {
        List<SystemConfig> toSave = request.getConfigs().stream()
                .map(entry -> {
                    SystemConfig config = systemConfigRepository.findById(entry.getConfigKey())
                            .orElse(SystemConfig.builder()
                                    .configKey(entry.getConfigKey())
                                    .build());
                    config.setConfigValue(entry.getConfigValue());
                    return config;
                })
                .toList();

        List<SystemConfig> saved = systemConfigRepository.saveAll(toSave);
        log.info("Admin updated {} system config(s)", saved.size());
        return saved.stream().map(this::toConfigResponse).toList();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /** Chuyển MIME type sang nhãn thân thiện để hiển thị trên dashboard. */
    private String resolveMimeLabel(String mimeType) {
        if (mimeType == null) return "Unknown";
        return switch (mimeType.toLowerCase()) {
            case "application/pdf"                                                          -> "PDF";
            case "application/msword",
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "Word";
            case "application/vnd.ms-excel",
                 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"       -> "Excel";
            case "application/vnd.ms-powerpoint",
                 "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "PowerPoint";
            case "text/plain"                                                               -> "Text";
            case "image/jpeg", "image/jpg"                                                 -> "JPEG";
            case "image/png"                                                               -> "PNG";
            case "image/gif"                                                               -> "GIF";
            default -> mimeType.contains("/") ? mimeType.split("/")[1].toUpperCase() : mimeType;
        };
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }

    private SystemConfigResponse toConfigResponse(SystemConfig config) {
        return SystemConfigResponse.builder()
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .description(config.getDescription())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    // =========================================================
    // DOCUMENT MANAGEMENT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getAllDocuments(
            String keyword,
            String subject,
            String major,
            DocumentVisibility visibility,
            Pageable pageable) {

        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        if (subject != null && subject.trim().isEmpty()) {
            subject = null;
        }
        if (major != null && major.trim().isEmpty()) {
            major = null;
        }

        return documentRepository.searchAndFilterAll(visibility, keyword, subject, major, pageable)
                .map(documentMapper::toResponse);
    }

    @Override
    @Transactional
    public MessageResponse deleteDocument(UUID documentId) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        doc.setDeletedAt(OffsetDateTime.now());
        documentRepository.save(doc);
        log.info("Admin soft-deleted violating document: id={}", documentId);
        return new MessageResponse("Tài liệu đã được xóa thành công");
    }
}
