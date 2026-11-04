package com.dormhub.repository;

import com.dormhub.entity.Room;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from Room room where room.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    List<Room> findByBuilding_Id(Long buildingId);
    List<Room> findByStatus(RoomStatus status);
    List<Room> findByRoomType(RoomType roomType);
    Optional<Room> findByBuilding_IdAndRoomNumber(Long buildingId, String roomNumber);
    
    // Pageable methods for REST API
    Page<Room> findByStatus(RoomStatus status, Pageable pageable);
    Page<Room> findByRoomType(RoomType roomType, Pageable pageable);
    Page<Room> findByBuilding_Id(Long buildingId, Pageable pageable);
    Page<Room> findByStatusAndBuilding_Id(RoomStatus status, Long buildingId, Pageable pageable);
    
    // Count methods
    long countByBuilding_Id(Long buildingId);
    long countByBuilding_IdAndStatus(Long buildingId, RoomStatus status);
}
