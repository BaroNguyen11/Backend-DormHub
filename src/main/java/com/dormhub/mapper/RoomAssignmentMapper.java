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
                // Thông tin phòng để dashboard hiển thị "Phòng của tôi"
                .roomNumber(assignment.getRoom().getRoomNumber())
                .buildingName(assignment.getRoom().getBuilding() == null
                        ? null : assignment.getRoom().getBuilding().getName())
                .floor(assignment.getRoom().getFloor())
                .capacity(assignment.getRoom().getCapacity())
                .currentOccupancy(assignment.getRoom().getCurrentOccupancy())
                .pricePerMonth(assignment.getRoom().getPricePerMonth())
                .build();
    }
}
