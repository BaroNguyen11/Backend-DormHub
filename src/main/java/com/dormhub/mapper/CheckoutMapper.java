package com.dormhub.mapper;

import com.dormhub.dto.response.CheckoutResponse;
import com.dormhub.entity.CheckoutRequest;
import org.springframework.stereotype.Component;

@Component
@lombok.RequiredArgsConstructor
public class CheckoutMapper {
    private final com.dormhub.repository.RoomAssignmentRepository roomAssignmentRepository;

    public CheckoutResponse toResponse(CheckoutRequest request) {
        if (request == null) {
            return null;
        }

        var student = request.getStudent();
        var stays = request.getRequestDate() == null ? java.util.List.<com.dormhub.entity.RoomAssignment>of()
                : roomAssignmentRepository.findActualAssignmentsForPeriod(request.getContract().getContractId(),
                        request.getRequestDate(), request.getRequestDate().plusNanos(1));
        var room = stays.isEmpty() ? null : stays.get(0).getRoom();
        return CheckoutResponse.builder()
                .checkoutRequestId(request.getCheckoutRequestId())
                .studentId(request.getStudent().getId())
                .contractId(request.getContract().getContractId())
                .roomId(room == null ? null : room.getId())
                .studentName(student.getUser() != null && student.getUser().getFullName() != null
                        ? student.getUser().getFullName() : student.getFullName())
                .studentCode(student.getStudentCode())
                .roomNumber(room == null ? null : room.getRoomNumber())
                .buildingName(room == null || room.getBuilding() == null ? null : room.getBuilding().getName())
                .requestDate(request.getRequestDate())
                .checkoutDate(request.getCheckoutDate())
                .status(request.getStatus())
                .reason(request.getReason())
                .processedAt(request.getProcessedAt())
                .note(request.getNote())
                .reviewedByName(request.getReviewedBy() == null
                        ? null : request.getReviewedBy().getFullName())
                .depositStatus(request.getContract().getDepositStatus())
                .build();
    }
}
