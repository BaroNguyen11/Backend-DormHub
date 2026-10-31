package com.dormhub.repository;

import com.dormhub.entity.Contract;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select contract from Contract contract where contract.contractId = :contractId")
    Optional<Contract> findByContractIdForUpdate(@Param("contractId") Long contractId);
}
