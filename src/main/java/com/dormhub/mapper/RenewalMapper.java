package com.dormhub.mapper;

import com.dormhub.dto.response.RenewalResponse;
import com.dormhub.entity.RenewalRequest;
import org.springframework.stereotype.Component;

@Component
@lombok.RequiredArgsConstructor
public class RenewalMapper {
    private final com.dormhub.repository.RoomAssignmentRepository roomAssignmentRepository;

    public RenewalResponse toResponse(RenewalRequest request) {
        if (request == null) {
            return null;
        }

        var contract = request.getContract();
        var student = contract.getStudent();
        var stays = request.getRequestedAt() == null ? java.util.List.<com.dormhub.entity.RoomAssignment>of()
                : roomAssignmentRepository.findActualAssignmentsForPeriod(contract.getContractId(),
                        request.getRequestedAt(), request.getRequestedAt().plusNanos(1));
        var room = stays.isEmpty() ? null : stays.get(0).getRoom();
        return RenewalResponse.builder()
                .renewalRequestId(request.getRenewalRequestId())
                .contractId(contract.getContractId())
                .studentId(contract.getStudent().getId())
                .studentName(student.getUser() != null && student.getUser().getFullName() != null
                        ? student.getUser().getFullName() : student.getFullName())
                .studentCode(student.getStudentCode())
                .roomNumber(room == null ? null : room.getRoomNumber())
                .buildingName(room == null || room.getBuilding() == null ? null : room.getBuilding().getName())
                .requestedEndDate(request.getRequestedEndDate())
                .contractEndDate(contract.getEndDate())
                .reason(request.getReason())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .reviewedById(request.getReviewedBy() == null ? null : request.getReviewedBy().getId())
                .reviewedAt(request.getReviewedAt())
                .reviewNote(request.getReviewNote())
                .build();
    }
}
