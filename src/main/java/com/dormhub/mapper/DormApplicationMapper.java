package com.dormhub.mapper;

import com.dormhub.dto.response.DormApplicationResponse;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Room;
import org.springframework.stereotype.Component;

@Component
public class DormApplicationMapper {

    public DormApplicationResponse toResponse(DormApplication application) {
        Room room = application.getRequestedRoom();
        return new DormApplicationResponse(
                application.getId(),
                application.getStudent().getId(),
                application.getStudent().getStudentCode(),
                application.getStudent().getUser().getFullName(),
                application.getStudent().getGender(),
                room == null ? null : room.getId(),
                room == null ? null : room.getRoomNumber(),
                room == null ? null : room.getBuilding().getId(),
                room == null ? null : room.getBuilding().getBuildingName(),
                application.getRequestDate(),
                application.getStatus(),
                application.getNote(),
                application.getReviewNote(),
                application.getProcessedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
