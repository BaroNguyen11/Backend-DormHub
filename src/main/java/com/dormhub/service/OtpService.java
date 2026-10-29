package com.dormhub.service;

import com.dormhub.entity.EmailVerification;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.OtpPurpose;
import com.dormhub.repository.EmailVerificationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private static final int OTP_EXPIRY_MINUTES = 5;

    private final EmailVerificationRepository
            emailVerificationRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String createOtp(User user, OtpPurpose purpose) {

        // 1. Xóa các OTP cũ chưa sử dụng
        emailVerificationRepository.deleteByUserIdAndPurposeAndUsedAtIsNull(user.getId(), purpose);

        // 2. Sinh OTP ngẫu nhiên gồm 6 chữ số
        String otp = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));

        // 3. Tạo bản ghi xác minh
        EmailVerification verification = new EmailVerification();

        verification.setUser(user);

        // Chỉ lưu hash, không lưu OTP gốc
        verification.setOtpHash(passwordEncoder.encode(otp));

        verification.setPurpose(purpose);

        // OTP hết hạn sau 5 phút
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));

        verification.setUsedAt(null);
        verification.setAttempts(0);

        emailVerificationRepository.save(verification);
        return otp;
    }
}