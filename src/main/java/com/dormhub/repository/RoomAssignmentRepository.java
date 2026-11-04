package com.dormhub.repository;

import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, Long> {

    boolean existsByRoom_Id(Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoomAssignment> findFirstByContract_ContractIdOrderByAssignmentIdDesc(Long contractId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoomAssignment> findFirstByStudent_IdAndStatusOrderByAssignmentIdDesc(
            Long studentId, RoomAssignmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from RoomAssignment assignment "
            + "where assignment.assignmentId = :assignmentId")
    Optional<RoomAssignment> findByAssignmentIdForUpdate(
            @Param("assignmentId") Long assignmentId);

    long countByRoom_IdAndStatusIn(Long roomId, Collection<RoomAssignmentStatus> statuses);
}
