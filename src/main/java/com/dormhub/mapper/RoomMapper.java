package com.dormhub.mapper;

import com.dormhub.dto.request.RoomRequest;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Room;
import com.dormhub.entity.enums.RoomStatus;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {
    
    public Room toEntity(RoomRequest request, Building building) {
        return Room.builder()
                .roomNumber(request.getRoomNumber())
                .floor(request.getFloor())
                .building(building)
                .roomType(request.getRoomType())
                .capacity(request.getCapacity())
                .currentOccupancy(0)
                .pricePerMonth(request.getPricePerMonth())
                .status(RoomStatus.AVAILABLE)
                .electricMeterNumber(request.getElectricMeterNumber())
                .waterMeterNumber(request.getWaterMeterNumber())
                .description(request.getDescription())
                .build();
    }
    
    public RoomResponse toResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .buildingId(room.getBuilding().getId())
                .buildingName(room.getBuilding().getName())
                .buildingCode(room.getBuilding().getBuildingCode())
                .roomType(room.getRoomType())
                .capacity(room.getCapacity())
                .currentOccupancy(room.getCurrentOccupancy())
                .pricePerMonth(room.getPricePerMonth())
                .status(room.getStatus())
                .electricMeterNumber(room.getElectricMeterNumber())
                .waterMeterNumber(room.getWaterMeterNumber())
                .description(room.getDescription())
                .isAvailable(room.getCurrentOccupancy() < room.getCapacity())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
