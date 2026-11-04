package com.dormhub.repository;

import com.dormhub.entity.Contract;
import com.dormhub.entity.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.Collection;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByStudent_IdAndStatusIn(Long studentId, Collection<ContractStatus> statuses);

    Page<Contract> findAllByStudent_Id(Long studentId, Pageable pageable);

    Page<Contract> findAllByStatus(ContractStatus status, Pageable pageable);

    Page<Contract> findAllByStudent_IdAndStatus(
            Long studentId, ContractStatus status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select contract from Contract contract where contract.contractId = :contractId")
    Optional<Contract> findByContractIdForUpdate(@Param("contractId") Long contractId);

    /**
     * Find active contract for a room in a specific billing month.
     * A contract is active if its date range includes the billing month.
     */
    @Query("SELECT c FROM Contract c " +
           "WHERE c.room.id = :roomId " +
           "AND c.status = 'ACTIVE' " +
           "AND :billingMonth BETWEEN " +
           "FUNCTION('TO_CHAR', c.startDate, 'YYYY-MM') " +
           "AND FUNCTION('TO_CHAR', c.endDate, 'YYYY-MM')")
    Optional<Contract> findActiveContractByRoomAndMonth(
        @Param("roomId") Long roomId,
        @Param("billingMonth") String billingMonth  // Format: "YYYY-MM" like "2026-11"
    );
}
