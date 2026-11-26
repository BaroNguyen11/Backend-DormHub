package com.dormhub.repository;

import com.dormhub.entity.SepayTransaction;
import com.dormhub.entity.enums.SepayTransactionStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SepayTransactionRepository extends JpaRepository<SepayTransaction, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<SepayTransaction> {

    boolean existsByCodeAndStatusIn(String code, Collection<SepayTransactionStatus> statuses);

    Optional<SepayTransaction> findBySepayTransactionId(Long sepayTransactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tx from SepayTransaction tx where tx.id = :id")
    Optional<SepayTransaction> findByIdForUpdate(@Param("id") Long id);

    java.util.List<SepayTransaction> findByStatusAndReceivedAtAfter(
            SepayTransactionStatus status, java.time.Instant receivedAt);

    Page<SepayTransaction> findAllByStatus(SepayTransactionStatus status, Pageable pageable);
}
