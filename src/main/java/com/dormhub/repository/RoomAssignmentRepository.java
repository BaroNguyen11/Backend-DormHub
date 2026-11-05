package com.dormhub.repository;

import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, Long> {

    boolean existsByStudent_IdAndStatusIn(Long studentId, Collection<RoomAssignmentStatus> statuses);

    boolean existsByContract_ContractIdAndStatusIn(
            Long contractId, Collection<RoomAssignmentStatus> statuses);

    boolean existsByRoom_IdAndStatusIn(Long roomId, Collection<RoomAssignmentStatus> statuses);

    boolean existsByRoom_Building_IdAndStatusIn(
            Long buildingId, Collection<RoomAssignmentStatus> statuses);

    boolean existsByRoom_Id(Long roomId);

    @EntityGraph(attributePaths = {"student", "contract", "room", "room.building", "assignedBy"})
    Page<RoomAssignment> findAllByStudent_Id(Long studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "contract", "room", "room.building", "assignedBy"})
    Page<RoomAssignment> findAllByStatus(RoomAssignmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "contract", "room", "room.building", "assignedBy"})
    Optional<RoomAssignment> findByAssignmentIdAndStudent_Id(Long assignmentId, Long studentId);

    @EntityGraph(attributePaths = {"student", "contract", "room", "room.building", "assignedBy"})
    Optional<RoomAssignment> findByAssignmentId(Long assignmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoomAssignment> findFirstByContract_ContractIdOrderByAssignmentIdDesc(
            Long contractId);

    @Query("select case when count(assignment) > 0 then true else false end "
            + "from RoomAssignment assignment "
            + "where assignment.room.id = :roomId "
            + "and assignment.student.user.id = :userId "
            + "and assignment.checkInAt is not null "
            + "and assignment.checkInAt < :monthEndExclusive "
            + "and (assignment.checkOutAt is null or assignment.checkOutAt > :monthStart)")
    boolean hasActualOccupancyOverlappingMonth(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEndExclusive") LocalDateTime monthEndExclusive);
    Optional<RoomAssignment> findFirstByStudent_IdAndStatusOrderByAssignmentIdDesc(
            Long studentId, RoomAssignmentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from RoomAssignment assignment "
            + "where assignment.assignmentId = :assignmentId")
    Optional<RoomAssignment> findByAssignmentIdForUpdate(
            @Param("assignmentId") Long assignmentId);

    long countByRoom_IdAndStatusIn(Long roomId, Collection<RoomAssignmentStatus> statuses);
}
