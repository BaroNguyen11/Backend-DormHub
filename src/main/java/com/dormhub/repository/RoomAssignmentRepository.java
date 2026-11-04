package com.dormhub.repository;

import com.dormhub.entity.RoomAssignment;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoomAssignment> findFirstByContract_ContractIdOrderByAssignmentIdDesc(Long contractId);

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
}
