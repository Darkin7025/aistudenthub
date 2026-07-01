package com.example.swp391.aistudenthub.feature.admin.service;

import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.admin.dto.request.UpdateSystemConfigRequest;
import com.example.swp391.aistudenthub.feature.admin.dto.request.UpdateUserStatusRequest;
import com.example.swp391.aistudenthub.feature.admin.dto.response.AdminUserResponse;
import com.example.swp391.aistudenthub.feature.admin.dto.response.SystemConfigResponse;
import com.example.swp391.aistudenthub.feature.admin.entity.SystemConfig;
import com.example.swp391.aistudenthub.feature.admin.repository.SystemConfigRepository;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;

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
}
