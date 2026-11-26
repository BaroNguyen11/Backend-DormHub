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
}
