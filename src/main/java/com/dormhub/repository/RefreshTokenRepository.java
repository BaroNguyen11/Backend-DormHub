
package com.dormhub.repository;

import com.dormhub.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUser_IdAndRevokedAtIsNull(Long userId);
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.revokedAt IS NOT NULL OR r.expiresAt < :now")
    int deleteAllExpiredOrRevoked(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
