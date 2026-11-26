package com.dormhub.dto.response;

import com.dormhub.entity.enums.TransferStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RoomTransferResponse {

    private final Long transferId;
    private final Long studentId;
    private final Long contractId;
    private final Long currentAssignmentId;
    private final Long currentRoomId;
    private final Long preferredRoomId;
    private final Long approvedRoomId;
    private final String studentName;
    private final String studentCode;
    private final String currentRoomNumber;
    private final String preferredRoomNumber;
    private final String approvedRoomNumber;
    private final TransferStatus status;
    private final String reason;
    private final LocalDateTime requestedAt;
    private final Long reviewedById;
    private final LocalDateTime reviewedAt;
    private final String reviewNote;
    private final LocalDateTime responseDeadline;
    private final LocalDateTime completionDeadline;
    private final LocalDateTime completedAt;
    private final boolean responseOverdue;
    private final boolean completionOverdue;
    private final boolean handoverActionsEnabled;
    private final boolean handoverPending;
    private final java.math.BigDecimal monthlyRent;
    private final java.math.BigDecimal depositAmount;
    private final com.dormhub.entity.enums.Gender studentGender;
    private final com.dormhub.entity.enums.ContractStatus contractStatus;
    private final LocalDateTime contractEndDate;
}
