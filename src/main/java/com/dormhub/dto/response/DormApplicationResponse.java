package com.dormhub.dto.response;

import com.dormhub.entity.DormApplication;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;

import java.time.LocalDateTime;

public record DormApplicationResponse(
        Long id,
        Long studentId,
        String studentCode,
        String studentName,
        Gender studentGender,
        Long requestedRoomId,
        String requestedRoomNumber,
        Long requestedBuildingId,
        String requestedBuildingName,
        LocalDateTime requestDate,
        DormApplicationStatus status,
        String note,
        String reviewNote,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DormApplicationResponse from(DormApplication application) {
        return new DormApplicationResponse(
                application.getId(),
                application.getStudent().getId(),
                application.getStudent().getStudentCode(),
                application.getStudent().getUser().getFullName(),
                application.getStudent().getGender(),
                application.getRequestedRoom() == null ? null : application.getRequestedRoom().getId(),
                application.getRequestedRoom() == null ? null : application.getRequestedRoom().getRoomNumber(),
                application.getRequestedRoom() == null ? null : application.getRequestedRoom().getBuilding().getId(),
                application.getRequestedRoom() == null ? null : application.getRequestedRoom().getBuilding().getBuildingName(),
                application.getRequestDate(),
                application.getStatus(),
                application.getNote(),
                application.getReviewNote(),
                application.getProcessedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
