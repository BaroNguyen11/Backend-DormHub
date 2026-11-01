package com.dormhub.mapper;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.entity.RoomAssignment;
import org.springframework.stereotype.Component;

@Component
public class RoomAssignmentMapper {

    public RoomAssignmentResponse toResponse(RoomAssignment assignment) {
        return new RoomAssignmentResponse(
                assignment.getId(),
                assignment.getDormApplication().getId(),
                assignment.getContract() == null ? null : assignment.getContract().getContractId(),
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
