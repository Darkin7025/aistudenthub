package com.example.swp391.aistudenthub.feature.auth.service;

import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.auth.dto.ChangePasswordRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.UpdateProfileRequest;
import com.example.swp391.aistudenthub.feature.auth.dto.UserProfileResponse;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import com.example.swp391.aistudenthub.feature.payment.repository.PaymentOrderRepository;
import com.example.swp391.aistudenthub.feature.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.Optional;
import com.example.swp391.aistudenthub.feature.payment.entity.PaymentOrder;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentOrderRepository paymentOrderRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return mapToResponse(findActiveUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (!user.getEmail().equals(normalizedEmail)
                && userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.setFullName(request.getFullName().trim());
        if (!user.getEmail().equals(normalizedEmail)) {
            user.setEmail(normalizedEmail);
            user.setEmailVerified(false);
        }
        user.setAvatarUrl(normalizeAvatarUrl(request.getAvatarUrl()));

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public MessageResponse changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findActiveUser(userId);

        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_MUST_BE_DIFFERENT);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new MessageResponse("Doi mat khau thanh cong.");
    }

    private User findActiveUser(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        return user;
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }
        return avatarUrl.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserProfileResponse mapToResponse(User user) {
        Optional<PaymentOrder> latestPaidOrder = paymentOrderRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), PaymentStatus.PAID);
        boolean isPremium = false;
        String subscriptionTier = "BASIC";
        java.time.OffsetDateTime premiumExpireAt = null;
        
        if (latestPaidOrder.isPresent()) {
            PaymentOrder order = latestPaidOrder.get();
            if (order.getPaidAt() != null) {
                java.time.OffsetDateTime calculatedExpireAt = order.getPaidAt().plusMonths(1);
                if (calculatedExpireAt.isAfter(java.time.OffsetDateTime.now())) {
                    isPremium = true;
                    premiumExpireAt = calculatedExpireAt;
                    int amount = order.getAmount();
                    if (amount >= 79000) {
                        subscriptionTier = "PREMIUM";
                    } else if (amount >= 39000) {
                        subscriptionTier = "PRO";
                    }
                }
            }
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isPremium(isPremium)
                .subscriptionTier(subscriptionTier)
                .premiumExpireAt(premiumExpireAt)
                .build();
    }
}
