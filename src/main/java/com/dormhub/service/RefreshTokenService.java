
package com.dormhub.service;

import com.dormhub.entity.RefreshToken;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.UserStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int TOKEN_BYTES = 32;
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String createRefreshToken(User user) {
        // 1. Sinh 32 byte ngẫu nhiên
        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);

        // 2. Chuyển thành chuỗi token có thể truyền qua HTTP
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // 3. Tạo entity và chỉ lưu hash của token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS));
        refreshToken.setRevokedAt(null);

        refreshTokenRepository.save(refreshToken);

        // 4. Trả token gốc để backend sử dụng tạo cookie
        return rawToken;
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không thể khởi tạo SHA-256", exception);
        }
    }
    public record RotationResult(
            User user,
            String refreshToken
    ) {}
    @Transactional
    public RotationResult rotateRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String tokenHash = hashToken(rawToken);

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        LocalDateTime now = LocalDateTime.now();

        if (currentToken.getRevokedAt() != null
                || !currentToken.getExpiresAt().isAfter(now)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = currentToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        // Thu hồi refresh token cũ
        currentToken.setRevokedAt(now);
        refreshTokenRepository.save(currentToken);

        // Tạo refresh token mới
        String newRefreshToken = createRefreshToken(user);

        return new RotationResult(user, newRefreshToken);
    }
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        // Không có refresh token thì không cần thu hồi
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        // Hash token để tìm bản ghi trong database
        String tokenHash = hashToken(rawToken);

        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    // Chỉ thu hồi nếu token chưa bị thu hồi
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    }
                });
    }

    /**
     * Thu hồi toàn bộ refresh token đang hiệu lực của một user
     * (dùng khi đổi mật khẩu hoặc khóa tài khoản).
     */
    @Transactional
    public void revokeAllForUser(Long userId) {
        if (userId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(userId)
                .forEach(token -> {
                    token.setRevokedAt(now);
                    refreshTokenRepository.save(token);
                });
    }
}
