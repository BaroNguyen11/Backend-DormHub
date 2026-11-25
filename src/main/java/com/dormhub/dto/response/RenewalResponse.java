package com.dormhub.dto.response;

import com.dormhub.entity.enums.RenewalStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RenewalResponse {

    private final Long renewalRequestId;
    private final Long contractId;
    private final Long studentId;
    private final String studentName;
    private final String studentCode;
    private final String roomNumber;
    private final String buildingName;
    private final LocalDateTime requestedEndDate;
    private final LocalDateTime contractEndDate;
    private final String reason;
    private final RenewalStatus status;
    private final LocalDateTime requestedAt;
    private final Long reviewedById;
    private final LocalDateTime reviewedAt;
    private final String reviewNote;
}
