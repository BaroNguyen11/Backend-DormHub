package com.dormhub.dto.response;

import com.dormhub.entity.RoomAssignment;
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

    public static RoomAssignmentResponse from(RoomAssignment assignment) {
        return new RoomAssignmentResponse(
                assignment.getId(),
                assignment.getDormApplication().getId(),
                assignment.getDormApplication().getStudent().getId(),
                assignment.getDormApplication().getStudent().getStudentCode(),
                assignment.getDormApplication().getStudent().getUser().getFullName(),
                assignment.getRoom().getId(),
                assignment.getRoom().getBuilding().getId(),
                assignment.getRoom().getBuilding().getBuildingName(),
                assignment.getRoom().getRoomNumber(),
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignment.getNote());
    }
}
