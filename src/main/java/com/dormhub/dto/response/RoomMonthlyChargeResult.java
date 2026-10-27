package com.dormhub.dto.response;

import java.math.BigDecimal;
import java.time.YearMonth;

public record RoomMonthlyChargeResult(
        Long roomId,
        YearMonth billingMonth,
        BigDecimal electricityConsumption,
        BigDecimal waterConsumption,
        BigDecimal electricityAmount,
        BigDecimal waterAmount,
        BigDecimal totalAmount) {
}
