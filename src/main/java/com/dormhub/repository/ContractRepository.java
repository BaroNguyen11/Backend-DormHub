package com.dormhub.repository;

import com.dormhub.entity.Contract;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.DepositStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByStudent_IdAndStatusIn(Long studentId, Collection<ContractStatus> statuses);

    @Query("select case when count(contract) > 0 then true else false end "
            + "from Contract contract "
            + "where contract.room.building.id = :buildingId "
            + "and contract.status in :statuses "
            + "and (contract.student.gender is null or contract.student.gender <> :gender)")
    boolean existsCurrentContractWithDifferentGender(
            @Param("buildingId") Long buildingId,
            @Param("statuses") Collection<ContractStatus> statuses,
            @Param("gender") Gender gender);

    @Query("select count(contract) from Contract contract "
            + "where contract.room.id = :roomId and contract.status in :statuses "
            + "and not exists (select assignment.assignmentId from RoomAssignment assignment "
            + "where assignment.contract = contract and assignment.status in ('ASSIGNED', 'CHECKED_IN'))")
    long countByRoom_IdAndStatusIn(@Param("roomId") Long roomId,
                                   @Param("statuses") Collection<ContractStatus> statuses);

    @Query("select contract.room.id as roomId, count(contract.contractId) as holdCount "
            + "from Contract contract "
            + "where contract.room.id in :roomIds and contract.status in :statuses "
            + "and not exists (select assignment.assignmentId from RoomAssignment assignment "
            + "where assignment.contract = contract and assignment.status in ('ASSIGNED', 'CHECKED_IN')) "
            + "group by contract.room.id")
    List<RoomHoldCountProjection> findRoomHoldCounts(
            @Param("roomIds") Collection<Long> roomIds,
            @Param("statuses") Collection<ContractStatus> statuses);

    interface RoomHoldCountProjection {
        Long getRoomId();

        Long getHoldCount();
    }

    @EntityGraph(attributePaths = {"student", "student.user", "room", "room.building"})
    Page<Contract> findAllByStudent_Id(Long studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "room.building"})
    Optional<Contract> findFirstByStudent_IdAndStatusOrderByContractIdDesc(
            Long studentId, ContractStatus status);

    @EntityGraph(attributePaths = {"student", "student.user", "room", "room.building"})
    Page<Contract> findAllByStatus(ContractStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "room", "room.building"})
    Page<Contract> findAllByStudent_IdAndStatus(
            Long studentId, ContractStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "room", "room.building"})
    Optional<Contract> findByDormApplication_Id(Long dormApplicationId);

    @EntityGraph(attributePaths = {"student", "student.user", "room", "room.building"})
    List<Contract> findAllByDormApplication_IdIn(List<Long> dormApplicationIds);

    @EntityGraph(attributePaths = {"student", "student.user", "room", "room.building"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select contract from Contract contract where contract.contractId = :contractId")
    Optional<Contract> findByContractIdForUpdate(@Param("contractId") Long contractId);

    /**
     * Find active contract for a room in a specific billing month.
     * A contract is active if its date range includes the billing month.
     */
    @EntityGraph(attributePaths = {"student", "student.user", "room", "room.building"})
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

    // ===== Vòng đời hợp đồng (auto-expire + nhắc hạn) =====

    @EntityGraph(attributePaths = {"student", "student.user", "room"})
    List<Contract> findAllByStatusAndEndDateBetween(
            ContractStatus status, LocalDateTime start, LocalDateTime end);

    /** Hết hạn hàng loạt hợp đồng ACTIVE đã quá endDate (idempotent, chỉ đích danh 1 cặp trạng thái). */
    @Modifying
    @Query("UPDATE Contract c SET c.status = :newStatus "
            + "WHERE c.status = :currentStatus AND c.endDate IS NOT NULL AND c.endDate < :now")
    int bulkExpireOverdueContracts(@Param("currentStatus") ContractStatus currentStatus,
                                   @Param("newStatus") ContractStatus newStatus,
                                   @Param("now") LocalDateTime now);

}
