package com.dormhub.repository;

import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, Long> {

    long countByRoomIdAndStatus(Long roomId, RoomAssignmentStatus status);

    boolean existsByRoomId(Long roomId);

    boolean existsByDormApplicationIdAndStatus(Long dormApplicationId, RoomAssignmentStatus status);

    boolean existsByDormApplicationStudentIdAndStatus(Long studentId, RoomAssignmentStatus status);

    boolean existsByRoomIdAndStatus(Long roomId, RoomAssignmentStatus status);

    boolean existsByRoomBuildingIdAndStatus(Long buildingId, RoomAssignmentStatus status);

    Page<RoomAssignment> findAllByStatus(RoomAssignmentStatus status, Pageable pageable);

    Page<RoomAssignment> findAllByDormApplicationStudentId(Long studentId, Pageable pageable);

    Optional<RoomAssignment> findByIdAndDormApplicationStudentId(Long id, Long studentId);

    @Query("select assignment.room.id, count(assignment.id) " +
            "from RoomAssignment assignment " +
            "where assignment.status = :status and assignment.room.id in :roomIds " +
            "group by assignment.room.id")
    List<Object[]> countByRoomIdsAndStatus(
            @Param("roomIds") Collection<Long> roomIds,
            @Param("status") RoomAssignmentStatus status);
}
