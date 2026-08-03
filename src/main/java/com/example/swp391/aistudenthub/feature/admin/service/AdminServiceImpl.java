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
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminBusinessStatsResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.RevenueTrendResponse;
import com.example.swp391.aistudenthub.feature.admin.entity.SystemConfig;
import com.example.swp391.aistudenthub.feature.admin.repository.SystemConfigRepository;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatSessionRepository;
import com.example.swp391.aistudenthub.feature.document.repository.DocumentRepository;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminDocumentResponse;
import com.example.swp391.aistudenthub.feature.document.dto.response.UploadStatusResponse;
import com.example.swp391.aistudenthub.feature.document.entity.Document;
import com.example.swp391.aistudenthub.feature.payment.repository.PaymentOrderRepository;
import com.example.swp391.aistudenthub.feature.payment.enums.PaymentStatus;
import com.example.swp391.aistudenthub.feature.payment.entity.PaymentOrder;
import com.example.swp391.aistudenthub.feature.document.enums.DocumentVisibility;
import com.example.swp391.aistudenthub.feature.document.enums.PreviewMode;
import com.example.swp391.aistudenthub.feature.document.service.DocumentPreviewResolver;
import com.example.swp391.aistudenthub.feature.document.service.DocumentService;
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
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.example.swp391.aistudenthub.feature.document.enums.UploadStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final SystemLogService systemLogService;
    private final DocumentService documentService;
    private final DocumentPreviewResolver previewResolver;
    private final PaymentOrderRepository paymentOrderRepository;

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
        long disabledUsers  = userRepository.countByActiveAndDeletedAtIsNull(false);

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

    @Override
    @Transactional(readOnly = true)
    public AdminBusinessStatsResponse getBusinessStats() {
        long totalRevenue = paymentOrderRepository.calculateTotalRevenue();
        
        OffsetDateTime startOfMonth = OffsetDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long currentMonthRevenue = paymentOrderRepository.calculateRevenueFromDate(startOfMonth);
        
        OffsetDateTime oneMonthAgo = OffsetDateTime.now().minusMonths(1);
        long activePremiumUsers = paymentOrderRepository.countActivePremiumUsers(oneMonthAgo);
        
        List<String> popularPackages = paymentOrderRepository.findMostPopularPackages();
        String mostPopularPackage = popularPackages.isEmpty() ? "Không có dữ liệu" : popularPackages.get(0);
        
        return AdminBusinessStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .currentMonthRevenue(currentMonthRevenue)
                .activePremiumUsers(activePremiumUsers)
                .mostPopularPackage(mostPopularPackage)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueTrendResponse> getRevenueTrend(int days) {
        if (days <= 0 || days > 365) days = 30;
        OffsetDateTime from = OffsetDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0).withNano(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<PaymentOrder> orders = paymentOrderRepository.findByStatusAndPaidAtAfterOrderByPaidAtAsc(PaymentStatus.PAID, from);
        
        // Group by date
        Map<String, Long> revenueByDate = new HashMap<>();
        for (PaymentOrder order : orders) {
            if (order.getPaidAt() != null) {
                String dateStr = order.getPaidAt().format(fmt);
                revenueByDate.put(dateStr, revenueByDate.getOrDefault(dateStr, 0L) + order.getAmount());
            }
        }
        
        // Fill missing days with 0
        return java.util.stream.IntStream.rangeClosed(0, days)
                .mapToObj(i -> from.plusDays(i).format(fmt))
                .map(dateStr -> RevenueTrendResponse.builder()
                        .date(dateStr)
                        .revenue(revenueByDate.getOrDefault(dateStr, 0L))
                        .build())
                .collect(Collectors.toList());
    }

    // =========================================================
    // SYSTEM CONFIG
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfigResponse> getAllConfigs() {
        return systemConfigRepository.findAllByOrderByConfigKeyAsc()
                .stream()
                .map(this::toConfigResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<SystemConfigResponse> updateConfigs(UpdateSystemConfigRequest request, UUID adminUserId, String adminEmail) {
        Set<String> configKeys = new HashSet<>();
        for (UpdateSystemConfigRequest.ConfigEntry entry : request.getConfigs()) {
            String key = entry.getConfigKey().trim();
            if (!configKeys.add(key)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Config key bị trùng: " + key);
            }
            entry.setConfigKey(key);
            entry.setConfigValue(entry.getConfigValue().trim());
            validateConfigValue(entry);
        }

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
        systemLogService.audit("SYSTEM_CONFIG_UPDATED", "Updated " + saved.size() + " system configuration value(s)",
                "AdminService", adminUserId, adminEmail, "SYSTEM_CONFIG", String.join(",", configKeys));
        log.info("Admin updated {} system config(s)", saved.size());
        return saved.stream().map(this::toConfigResponse).toList();
    }

    // =========================================================
    // DOCUMENT MANAGEMENT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminDocumentResponse> getAllDocuments(
            UUID userId,
            String keyword,
            String subject,
            String major,
            String documentType,
            UploadStatus uploadStatus,
            DocumentVisibility visibility,
            Boolean includeDeleted,
            Pageable pageable) {
        if (keyword != null && keyword.trim().isEmpty()) keyword = null;
        if (subject != null && subject.trim().isEmpty()) subject = null;
        if (major != null && major.trim().isEmpty()) major = null;
        if (documentType != null && documentType.trim().isEmpty()) documentType = null;
        if (includeDeleted == null) includeDeleted = false;

        // Nếu Pageable chưa có sắp xếp, mặc định sắp xếp theo createdAt DESC (mới nhất lên đầu)
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Page<Document> documents = documentRepository.searchAllDocumentsAdmin(
                        userId, keyword, subject, major, documentType, uploadStatus, visibility, includeDeleted, pageable);

        List<UUID> userIds = documents.getContent().stream()
                .map(Document::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, User> usersMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            usersMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, java.util.function.Function.identity()));
        }

        final Map<UUID, User> finalUsersMap = usersMap;
        return documents.map(doc -> toAdminDocumentResponse(doc, finalUsersMap.get(doc.getUserId())));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDocumentResponse getDocumentById(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
        return toAdminDocumentResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public UploadStatusResponse getDocumentUploadStatus(UUID documentId, User adminUser) {
        return documentService.getUploadStatus(documentId, adminUser);
    }

    @Override
    @Transactional
    public MessageResponse softDeleteDocumentByAdmin(UUID documentId, UUID adminUserId, String adminEmail) {
        Document doc = documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        doc.setDeletedAt(OffsetDateTime.now());
        documentRepository.save(doc);

        systemLogService.audit("DOCUMENT_DELETED_BY_ADMIN",
                "Deleted document: " + doc.getTitle() + " (ID: " + documentId + ")",
                "AdminService", adminUserId, adminEmail, "DOCUMENT", documentId.toString());

        log.info("Admin {} soft-deleted document {}", adminEmail, documentId);
        return new MessageResponse("Tài liệu đã được xóa bởi Admin thành công");
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private AdminDocumentResponse toAdminDocumentResponse(Document doc) {
        User user = null;
        if (doc != null && doc.getUserId() != null) {
            user = userRepository.findById(doc.getUserId()).orElse(null);
        }
        return toAdminDocumentResponse(doc, user);
    }

    private AdminDocumentResponse toAdminDocumentResponse(Document doc, User user) {
        if (doc == null) return null;

        String uploaderEmail = null;
        String uploaderFullName = null;
        if (user != null) {
            uploaderEmail = user.getEmail();
            uploaderFullName = user.getFullName();
        }

        PreviewMode previewMode = previewResolver.resolveMode(doc.getOriginalFileName(), doc.getFileType());
        boolean aiSupported = previewResolver.isAiCapable(previewMode)
                && org.springframework.util.StringUtils.hasText(doc.getExtractedText());

        return AdminDocumentResponse.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .uploaderEmail(uploaderEmail)
                .uploaderFullName(uploaderFullName)
                .title(doc.getTitle())
                .description(doc.getDescription())
                .fileUrl(doc.getFileUrl())
                .fileName(doc.getFileName())
                .originalFileName(doc.getOriginalFileName())
                .fileSize(doc.getFileSize())
                .fileType(doc.getFileType())
                .previewMode(previewMode != null ? previewMode.name() : null)
                .aiSupported(aiSupported)
                .visibility(doc.getVisibility())
                .subject(doc.getSubject())
                .major(doc.getMajor())
                .documentType(doc.getDocumentType())
                .uploadStatus(doc.getUploadStatus())
                .uploadProgress(doc.getUploadProgress())
                .folderId(doc.getFolderId())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .deletedAt(doc.getDeletedAt())
                .customMetadata(doc.getCustomMetadata())
                .build();
    }

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

    private void validateConfigValue(UpdateSystemConfigRequest.ConfigEntry entry) {
        String key = entry.getConfigKey();
        String value = entry.getConfigValue();
        if (key.startsWith("feature.") && key.endsWith(".enabled")
                && !("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Giá trị của feature flag phải là true hoặc false: " + key);
        }
        if ("system.max_file_size_mb".equals(key)) {
            try {
                int maxFileSize = Integer.parseInt(value);
                if (maxFileSize < 1 || maxFileSize > 10) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "system.max_file_size_mb phải là số nguyên từ 1 đến 10");
            }
        }
        if ("system.ai_daily_question_limit".equals(key)) {
            try {
                int dailyLimit = Integer.parseInt(value);
                if (dailyLimit < 1 || dailyLimit > 10000) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "system.ai_daily_question_limit must be an integer from 1 to 10000");
            }
        }
    }
}
