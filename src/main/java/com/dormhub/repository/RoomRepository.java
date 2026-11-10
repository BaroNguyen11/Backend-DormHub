package com.dormhub.repository;

import com.dormhub.entity.Room;
import com.dormhub.entity.enums.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    @Query("select distinct room from Room room "
            + "left join fetch room.building "
            + "left join fetch room.roomAssignments assignment "
            + "left join fetch assignment.student student "
            + "left join fetch student.user "
            + "where room.id = :id")
    Optional<Room> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from Room room where room.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "building")
    List<Room> findByStatus(RoomStatus status);
    Optional<Room> findByBuilding_IdAndRoomNumber(Long buildingId, String roomNumber);
    
    // Count methods
    long countByBuilding_Id(Long buildingId);
    
    long countByStatus(RoomStatus status);

    @Query("select room.building.id as buildingId, count(room.id) as totalRooms, "
            + "coalesce(sum(case when room.status = :status then 1 else 0 end), 0) as availableRooms "
            + "from Room room group by room.building.id")
    List<BuildingRoomStatsProjection> findBuildingRoomStats(@Param("status") RoomStatus status);

    @Query("select :buildingId as buildingId, count(room.id) as totalRooms, "
            + "coalesce(sum(case when room.status = :status then 1 else 0 end), 0) as availableRooms "
            + "from Room room where room.building.id = :buildingId")
    BuildingRoomStatsProjection getBuildingRoomStats(
            @Param("buildingId") Long buildingId,
            @Param("status") RoomStatus status);

    interface BuildingRoomStatsProjection {
        Long getBuildingId();

        Long getTotalRooms();

        Long getAvailableRooms();
    }
}
