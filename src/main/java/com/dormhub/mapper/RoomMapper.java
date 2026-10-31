package com.dormhub.mapper;

import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.Room;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

    public RoomResponse toResponse(Room room, int availableSlots) {
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
