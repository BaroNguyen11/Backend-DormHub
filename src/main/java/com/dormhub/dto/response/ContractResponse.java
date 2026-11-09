package com.dormhub.dto.response;

import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContractResponse {

    private final Long contractId;
    private final Long roomId;
    private final Long studentId;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final BigDecimal monthlyRent;
    private final BigDecimal depositAmount;
    private final ContractStatus status;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String studentName;
    private final String studentCode;
    private final String roomNumber;
    private final String buildingName;
    private final DepositStatus depositStatus;
    private final RoomAssignmentStatus assignmentStatus;
}
