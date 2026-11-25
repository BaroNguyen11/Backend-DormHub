package com.dormhub.mapper;

import com.dormhub.dto.response.RoomTransferResponse;
import com.dormhub.entity.RoomTransferRequest;
import org.springframework.stereotype.Component;

@Component
public class RoomTransferMapper {

    public RoomTransferResponse toResponse(RoomTransferRequest request) {
        if (request == null) {
            return null;
        }

        var assignment = request.getCurrentAssignment();
        var student = request.getStudent();
        return RoomTransferResponse.builder()
                .transferId(request.getTransferId())
                .studentId(request.getStudent().getId())
                .studentName(student.getUser() != null && student.getUser().getFullName() != null
                        ? student.getUser().getFullName() : student.getFullName())
                .studentCode(student.getStudentCode())
                .currentRoomNumber(assignment.getRoom().getRoomNumber())
                .preferredRoomNumber(request.getPreferredRoom() == null ? null : request.getPreferredRoom().getRoomNumber())
                .approvedRoomNumber(request.getApprovedRoom() == null ? null : request.getApprovedRoom().getRoomNumber())
                .contractId(assignment.getContract().getContractId())
                .currentAssignmentId(assignment.getAssignmentId())
                .currentRoomId(assignment.getRoom().getId())
                .preferredRoomId(request.getPreferredRoom() == null
                        ? null : request.getPreferredRoom().getId())
                .approvedRoomId(request.getApprovedRoom() == null
                        ? null : request.getApprovedRoom().getId())
                .status(request.getStatus())
                .reason(request.getReason())
                .requestedAt(request.getRequestedAt())
                .reviewedById(request.getReviewedBy() == null
                        ? null : request.getReviewedBy().getId())
                .reviewedAt(request.getReviewedAt())
                .reviewNote(request.getReviewNote())
                .build();
    }
}
