package com.dormhub.repository;

import com.dormhub.entity.SepayTransaction;
import com.dormhub.entity.enums.SepayTransactionStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SepayTransactionRepository extends JpaRepository<SepayTransaction, Long> {

    Optional<SepayTransaction> findBySepayTransactionId(Long sepayTransactionId);

    Page<SepayTransaction> findAllByStatus(SepayTransactionStatus status, Pageable pageable);
}
