package com.example.swp391.aistudenthub.feature.auth.service;

import com.example.swp391.aistudenthub.feature.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Dọn dẹp các refresh token đã hết hạn hoặc bị thu hồi (revoked).
     * Chạy định kỳ vào lúc 0h00 hàng ngày.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired and revoked refresh tokens...");
        try {
            refreshTokenRepository.deleteExpiredOrRevoked(OffsetDateTime.now());
            log.info("Cleanup of expired and revoked refresh tokens completed successfully.");
        } catch (Exception e) {
            log.error("Failed to clean up expired refresh tokens: {}", e.getMessage(), e);
        }
    }
}
