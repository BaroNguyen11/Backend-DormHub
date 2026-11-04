package com.dormhub.repository;

import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

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
}
