package com.dormhub.mapper;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.entity.RoomAssignment;
import org.springframework.stereotype.Component;

@Component
public class RoomAssignmentMapper {

    public RoomAssignmentResponse toResponse(RoomAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        return RoomAssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .contractId(assignment.getContract().getContractId())
                .studentId(assignment.getStudent().getId())
                .roomId(assignment.getRoom().getId())
                .assignedById(assignment.getAssignedBy() == null
                        ? null : assignment.getAssignedBy().getId())
                .assignmentStatus(assignment.getStatus())
                .contractStatus(assignment.getContract().getStatus())
                .assignedAt(assignment.getAssignedAt())
                .checkInAt(assignment.getCheckInAt())
                .checkOutAt(assignment.getCheckOutAt())
                .build();
    }
}
