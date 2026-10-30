package com.dormhub.dto.response;

import java.math.BigDecimal;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoomMonthlyChargeResult {
    private final Long roomId;
    private final YearMonth billingMonth;
    private final BigDecimal electricityConsumption;
    private final BigDecimal waterConsumption;
    private final BigDecimal electricityAmount;
    private final BigDecimal waterAmount;
    private final BigDecimal totalAmount;
}
