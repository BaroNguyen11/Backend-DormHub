package com.dormhub.repository;

import com.dormhub.entity.CheckoutRequest;
import com.dormhub.entity.enums.CheckoutRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckoutRequestRepository extends JpaRepository<CheckoutRequest, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<CheckoutRequest> {
    @Query("select count(r) > 0 from CheckoutRequest r where r.student.id = :studentId "
            + "and r.status in ('PENDING', 'APPROVED') and r.checkoutRequestId <> :excludedId")
    boolean hasOpenRequest(@Param("studentId") Long studentId, @Param("excludedId") Long excludedId);

    boolean existsByContract_ContractIdAndStatusIn(
            Long contractId, Collection<CheckoutRequestStatus> statuses);

    Page<CheckoutRequest> findAllByStudent_Id(Long studentId, Pageable pageable);

    Optional<CheckoutRequest> findByCheckoutRequestIdAndStudent_Id(
            Long checkoutRequestId, Long studentId);

    Page<CheckoutRequest> findAllByStatus(CheckoutRequestStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from CheckoutRequest request "
            + "where request.checkoutRequestId = :checkoutRequestId")
    Optional<CheckoutRequest> findByCheckoutRequestIdForUpdate(
            @Param("checkoutRequestId") Long checkoutRequestId);
}
