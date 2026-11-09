package com.dormhub.dto.response;

import com.dormhub.entity.enums.CheckoutRequestStatus;
import com.dormhub.entity.enums.DepositStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CheckoutResponse {

    private final Long checkoutRequestId;
    private final Long studentId;
    private final Long contractId;
    private final Long roomId;
    private final LocalDateTime requestDate;
    private final LocalDate checkoutDate;
    private final CheckoutRequestStatus status;
    private final String reason;
    private final LocalDateTime processedAt;
    private final String note;
    private final String reviewedByName;
    private final DepositStatus depositStatus;
}
