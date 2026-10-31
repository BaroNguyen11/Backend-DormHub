package com.dormhub.dto.response;

import com.dormhub.entity.enums.RoomAssignmentStatus;

import java.time.LocalDateTime;

public record RoomAssignmentResponse(
        Long id,
        Long dormApplicationId,
        Long studentId,
        String studentCode,
        String studentName,
        Long roomId,
        Long buildingId,
        String buildingName,
        String roomNumber,
        RoomAssignmentStatus status,
        LocalDateTime assignedAt,
        String note) {
}
