package com.dormhub.service;

import com.dormhub.repository.EmailVerificationRepository;
import com.dormhub.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    // Chạy định kỳ vào 02:00 sáng mỗi ngày
    @Scheduled(cron = "${dormhub.scheduler.token.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Scheduler TokenCleanupTask.cleanupExpiredTokens START at={}", java.time.Instant.now());
        try {
        LocalDateTime now = LocalDateTime.now();

        int deletedTokens = refreshTokenRepository.deleteAllExpiredOrRevoked(now);
        int deletedOtps = emailVerificationRepository.deleteAllExpiredOrUsed(now);

        log.info("Dọn dẹp DB hoàn tất: Đã xóa {} refresh tokens và {} OTPs hết hạn.", deletedTokens, deletedOtps);
        } finally {
            log.info("Scheduler TokenCleanupTask.cleanupExpiredTokens END at={}", java.time.Instant.now());
        }
    }
}
