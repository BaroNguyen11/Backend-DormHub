package com.dormhub.repository;

import com.dormhub.entity.Contract;
import com.dormhub.entity.enums.ContractStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByDormApplicationId(Long dormApplicationId);

    List<Contract> findAllByDormApplicationIdIn(Collection<Long> dormApplicationIds);

    boolean existsByStudentIdAndStatusIn(Long studentId, Collection<ContractStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select contract from Contract contract where contract.contractId = :id")
    Optional<Contract> findByIdForUpdate(@Param("id") Long id);
}
