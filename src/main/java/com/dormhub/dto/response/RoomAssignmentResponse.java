package com.dormhub.dto.response;

import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RoomAssignmentResponse {

    private final Long assignmentId;
    private final Long contractId;
    private final Long studentId;
    private final Long roomId;
    private final Long assignedById;
    private final RoomAssignmentStatus assignmentStatus;
    private final ContractStatus contractStatus;
    private final LocalDateTime assignedAt;
    private final LocalDateTime checkInAt;
    private final LocalDateTime checkOutAt;

    // ===== Thông tin phòng (phục vụ màn hình "Phòng của tôi") =====
    private final String roomNumber;
    private final String buildingName;
    private final Integer floor;
    private final Integer capacity;
    private final Integer currentOccupancy;
    private final BigDecimal pricePerMonth;
}
