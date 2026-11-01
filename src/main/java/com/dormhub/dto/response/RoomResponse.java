package com.dormhub.dto.response;

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
        Integer currentOccupancy,
        Integer reservedOccupancy,
        Integer availableSlots,
        String roomType,
        BigDecimal price,
        RoomStatus status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
