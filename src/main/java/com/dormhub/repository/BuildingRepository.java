package com.dormhub.repository;

import com.dormhub.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {
    boolean existsByBuildingCode(String buildingCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select building from Building building where building.id = :id")
    Optional<Building> findByIdForUpdate(@Param("id") Long id);
}
