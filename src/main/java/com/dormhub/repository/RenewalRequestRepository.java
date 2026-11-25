package com.dormhub.repository;

import com.dormhub.entity.RenewalRequest;
import com.dormhub.entity.enums.RenewalStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RenewalRequestRepository extends JpaRepository<RenewalRequest, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<RenewalRequest> {

    boolean existsByContract_ContractIdAndStatus(Long contractId, RenewalStatus status);

    Page<RenewalRequest> findAllByContract_Student_Id(Long studentId, Pageable pageable);

    Optional<RenewalRequest> findByRenewalRequestIdAndContract_Student_Id(
            Long renewalRequestId, Long studentId);

    Page<RenewalRequest> findAllByStatus(RenewalStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from RenewalRequest request "
            + "where request.renewalRequestId = :renewalRequestId")
    Optional<RenewalRequest> findByRenewalRequestIdForUpdate(
            @Param("renewalRequestId") Long renewalRequestId);
}
