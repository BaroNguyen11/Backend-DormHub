package com.dormhub.repository;

import com.dormhub.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from Room room where room.id = :id")
    java.util.Optional<Room> findByIdForUpdate(@Param("id") Long id);

    boolean existsByBuildingId(Long buildingId);

    boolean existsByBuildingIdAndRoomNumber(Long buildingId, String roomNumber);

    boolean existsByBuildingIdAndRoomNumberAndIdNot(Long buildingId, String roomNumber, Long id);
}
