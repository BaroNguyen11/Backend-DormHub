package com.dormhub.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckInPlanResponse {
    private final Long contractId;
    private final String roomNumber;
    private final String buildingName;
    private final LocalDateTime depositPaidAt;
    private final LocalDate earliestDate;
    private final LocalDate latestDate;
    private final LocalDate checkInDate;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal monthlyRent;
    private final BigDecimal firstMonthRent;
    private final boolean confirmed;
}
