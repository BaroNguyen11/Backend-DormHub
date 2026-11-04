package com.dormhub.repository;

import com.dormhub.entity.RoomAssignment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RoomAssignment> findFirstByContract_ContractIdOrderByAssignmentIdDesc(Long contractId);
}
