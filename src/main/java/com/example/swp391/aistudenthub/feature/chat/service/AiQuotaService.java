package com.example.swp391.aistudenthub.feature.chat.service;

import com.example.swp391.aistudenthub.exception.AppException;
import com.example.swp391.aistudenthub.exception.ErrorCode;
import com.example.swp391.aistudenthub.feature.admin.repository.SystemConfigRepository;
import com.example.swp391.aistudenthub.feature.auth.repository.UserRepository;
import com.example.swp391.aistudenthub.feature.chat.dto.response.ChatQuotaResponse;
import com.example.swp391.aistudenthub.feature.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiQuotaService {

    public static final String DAILY_LIMIT_CONFIG_KEY = "system.ai_daily_question_limit";
    public static final int DEFAULT_DAILY_LIMIT = 20;

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SystemConfigRepository systemConfigRepository;

    /** Reserves one question while holding the user row lock to prevent concurrent bypasses. */
    @Transactional
    public ChatQuotaResponse reserveQuestion(UUID userId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        ChatQuotaResponse status = buildStatus(userId);
        if (status.getUsed() >= status.getDailyLimit()) {
            throw new AppException(ErrorCode.LIMIT_EXCEEDED,
                    String.format("You have used all %d AI questions for today. Reset at %s.",
                            status.getDailyLimit(), status.getResetAt()));
        }
        return status;
    }

    @Transactional(readOnly = true)
    public ChatQuotaResponse getStatus(UUID userId) {
        userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return buildStatus(userId);
    }

    private ChatQuotaResponse buildStatus(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        OffsetDateTime resetAt = startOfDay.plusDays(1);
        int limit = resolveDailyLimit();
        long used = chatMessageRepository.countUserQuestionsSince(userId, startOfDay);

        return ChatQuotaResponse.builder()
                .dailyLimit(limit)
                .used(used)
                .remaining(Math.max(0, limit - used))
                .resetAt(resetAt)
                .build();
    }

    private int resolveDailyLimit() {
        return systemConfigRepository.findById(DAILY_LIMIT_CONFIG_KEY)
                .map(config -> parseLimit(config.getConfigValue()))
                .orElse(DEFAULT_DAILY_LIMIT);
    }

    private int parseLimit(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : DEFAULT_DAILY_LIMIT;
        } catch (Exception ignored) {
            return DEFAULT_DAILY_LIMIT;
        }
    }
}
