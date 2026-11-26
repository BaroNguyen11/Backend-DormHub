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

    boolean existsByStudent_IdAndStatus(Long studentId, TransferStatus status);

    Page<RoomTransferRequest> findAllByStudent_Id(Long studentId, Pageable pageable);

    Optional<RoomTransferRequest> findByTransferIdAndStudent_Id(Long transferId, Long studentId);

    Page<RoomTransferRequest> findAllByStatus(TransferStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from RoomTransferRequest request where request.transferId = :transferId")
    Optional<RoomTransferRequest> findByTransferIdForUpdate(@Param("transferId") Long transferId);
}
