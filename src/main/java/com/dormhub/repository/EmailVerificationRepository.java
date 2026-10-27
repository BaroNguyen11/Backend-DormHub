package com.dormhub.repository;


import com.dormhub.entity.EmailVerification;
import com.dormhub.entity.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findTopByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);
    void deleteByUserIdAndPurposeAndUsedAtIsNull(Long userId, OtpPurpose purpose);
    Optional<EmailVerification> findTopByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(Long id,OtpPurpose purpose);
}