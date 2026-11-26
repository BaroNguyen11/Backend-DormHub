package com.dormhub.repository;

import com.dormhub.entity.RoomTransferRequest;
import com.dormhub.entity.enums.TransferStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTransferRequestRepository
        extends JpaRepository<RoomTransferRequest, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<RoomTransferRequest> {
    @Query("select count(r) from RoomTransferRequest r where r.status='APPROVED' "
            + "and r.approvedRoom.id=:roomId and r.currentAssignment.status='CHECKED_IN'")
    long countApprovedHolds(@Param("roomId") Long roomId);

    @Query("select r.approvedRoom.id as roomId, count(r) as holdCount from RoomTransferRequest r "
            + "where r.status='APPROVED' and r.approvedRoom.id in :roomIds "
            + "and r.currentAssignment.status='CHECKED_IN' group by r.approvedRoom.id")
    java.util.List<ContractRepository.RoomHoldCountProjection> findApprovedHoldCounts(
            @Param("roomIds") java.util.Collection<Long> roomIds);

    @Query("select count(r)>0 from RoomTransferRequest r where r.status='APPROVED' and ("
            + "r.approvedRoom is null or r.reviewedAt is null or r.reviewedBy is null "
            + "or r.currentAssignment.status<>'CHECKED_IN' or r.currentAssignment.checkInAt is null "
            + "or r.currentAssignment.student.id<>r.student.id "
            + "or r.currentAssignment.contract.student.id<>r.student.id "
            + "or r.approvedRoom.id=r.currentAssignment.room.id "
            + "or r.currentAssignment.room.id<>r.currentAssignment.contract.room.id)")
    boolean hasIncompatibleApprovedTransfers();

    @Query(value = "select not exists (select 1 from pg_constraint c "
            + "where c.conrelid='public.room_transfer_requests'::regclass and c.contype='c' "
            + "and (select attnum from pg_attribute where attrelid=c.conrelid and attname='status')=any(c.conkey) "
            + "and position('COMPLETED' in pg_get_constraintdef(c.oid))=0)", nativeQuery = true)
    boolean supportsCompletedStatus();

    @Query("select count(r) > 0 from RoomTransferRequest r where r.student.id = :studentId "
            + "and r.transferId <> :excludedId and (r.status='PENDING' "
            + "or (r.status='APPROVED' and r.currentAssignment.status='CHECKED_IN'))")
    boolean hasOpenRequest(@Param("studentId") Long studentId, @Param("excludedId") Long excludedId);

    boolean existsByStudent_IdAndStatus(Long studentId, TransferStatus status);

    Page<RoomTransferRequest> findAllByStudent_Id(Long studentId, Pageable pageable);

    Optional<RoomTransferRequest> findByTransferIdAndStudent_Id(Long transferId, Long studentId);

    Page<RoomTransferRequest> findAllByStatus(TransferStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from RoomTransferRequest request where request.transferId = :transferId")
    Optional<RoomTransferRequest> findByTransferIdForUpdate(@Param("transferId") Long transferId);
}
