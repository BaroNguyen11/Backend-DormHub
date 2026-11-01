package com.dormhub.repository;

import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, Long> {

    boolean existsByRoomId(Long roomId);

    boolean existsByDormApplicationIdAndStatus(Long dormApplicationId, RoomAssignmentStatus status);

    boolean existsByContractContractIdAndStatus(Long contractId, RoomAssignmentStatus status);

    boolean existsByDormApplicationStudentIdAndStatus(Long studentId, RoomAssignmentStatus status);

    boolean existsByRoomIdAndStatus(Long roomId, RoomAssignmentStatus status);

    boolean existsByRoomBuildingIdAndStatus(Long buildingId, RoomAssignmentStatus status);

    Page<RoomAssignment> findAllByStatus(RoomAssignmentStatus status, Pageable pageable);

    Page<RoomAssignment> findAllByDormApplicationStudentId(Long studentId, Pageable pageable);

    Optional<RoomAssignment> findByIdAndDormApplicationStudentId(Long id, Long studentId);

}
