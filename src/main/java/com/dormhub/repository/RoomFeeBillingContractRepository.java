package com.dormhub.repository;

import com.dormhub.entity.Contract;
import com.dormhub.entity.enums.ContractStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomFeeBillingContractRepository extends JpaRepository<Contract, Long> {

    @Query("select max(c.contractId) from Contract c")
    Long findHighestContractId();

    @Query("select min(c.startDate) from Contract c where c.status = 'ACTIVE'")
    LocalDateTime findEarliestActiveStart();

    @Query("select c.contractId from Contract c "
            + "where c.contractId > :afterId and c.contractId <= :highestId "
            + "and c.status = :status "
            + "and c.startDate < :nextMonthStart and c.startDate <= :now "
            + "and c.endDate >= :monthStart order by c.contractId asc")
    List<Long> findEligibleIdsAfter(
            @Param("afterId") Long afterId,
            @Param("highestId") Long highestId,
            @Param("status") ContractStatus status,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("nextMonthStart") LocalDateTime nextMonthStart,
            @Param("now") LocalDateTime now,
            Pageable pageable);
}
