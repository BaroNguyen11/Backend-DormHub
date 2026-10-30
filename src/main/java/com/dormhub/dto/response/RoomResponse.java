package com.dormhub.dto.response;

import com.dormhub.entity.Room;
import com.dormhub.entity.enums.BuildingGenderPolicy;
import com.dormhub.entity.enums.RoomStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoomResponse(
        Long id,
        Long buildingId,
        String buildingName,
        BuildingGenderPolicy buildingGenderPolicy,
        String roomNumber,
        Integer floor,
        Integer capacity,
        Integer availableSlots,
        String roomType,
        BigDecimal price,
        RoomStatus status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static RoomResponse from(Room room, int availableSlots) {
        return new RoomResponse(
                room.getId(),
                room.getBuilding().getId(),
                room.getBuilding().getBuildingName(),
                room.getBuilding().getGenderPolicy(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getCapacity(),
                availableSlots,
                room.getRoomType(),
                room.getPrice(),
                room.getStatus(),
                room.getDescription(),
                room.getCreatedAt(),
                room.getUpdatedAt());
    }
}
