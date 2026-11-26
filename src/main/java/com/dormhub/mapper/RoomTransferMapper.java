package com.dormhub.mapper;

import com.dormhub.dto.response.RoomTransferResponse;
import com.dormhub.entity.RoomTransferRequest;
import org.springframework.stereotype.Component;

@Component
@lombok.RequiredArgsConstructor
public class RoomTransferMapper {
    private final com.dormhub.service.RoomStatusService roomStatusService;

    public RoomTransferResponse toResponse(RoomTransferRequest request) {
        if (request == null) {
            return null;
        }

        var assignment = request.getCurrentAssignment();
        var student = request.getStudent();
        var now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        var responseDeadline = request.getRequestedAt() == null ? null : request.getRequestedAt().plusHours(48);
        var completionDeadline = request.getReviewedAt() == null
                || !(request.getStatus() == com.dormhub.entity.enums.TransferStatus.APPROVED
                    || request.getStatus() == com.dormhub.entity.enums.TransferStatus.COMPLETED)
                ? null : request.getReviewedAt().plusHours(72);
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
                .responseDeadline(responseDeadline)
                .completionDeadline(completionDeadline)
                .completedAt(request.getStatus() == com.dormhub.entity.enums.TransferStatus.COMPLETED ? assignment.getCheckOutAt() : null)
                .responseOverdue(request.getStatus() == com.dormhub.entity.enums.TransferStatus.PENDING
                        && responseDeadline != null && now.isAfter(responseDeadline))
                .completionOverdue(request.getStatus() == com.dormhub.entity.enums.TransferStatus.APPROVED
                        && completionDeadline != null && now.isAfter(completionDeadline))
                .handoverActionsEnabled(roomStatusService.isTransferHandoverReady())
                .handoverPending(request.getStatus() == com.dormhub.entity.enums.TransferStatus.APPROVED
                        && assignment.getStatus() == com.dormhub.entity.enums.RoomAssignmentStatus.CHECKED_IN)
                .monthlyRent(assignment.getContract().getMonthlyRent())
                .depositAmount(assignment.getContract().getDepositAmount())
                .studentGender(student.getGender())
                .contractStatus(assignment.getContract().getStatus())
                .contractEndDate(assignment.getContract().getEndDate())
                .build();
    }
}
