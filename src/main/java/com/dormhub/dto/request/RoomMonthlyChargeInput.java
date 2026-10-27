package com.dormhub.dto.request;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Data from a confirmed meter reading, supplied by its owning module. */
public record RoomMonthlyChargeInput(
        Long roomId,
        YearMonth billingMonth,
        boolean confirmed,
        BigDecimal electricityOld,
        BigDecimal electricityNew,
        BigDecimal electricityUnitPrice,
        BigDecimal waterOld,
        BigDecimal waterNew,
        BigDecimal waterUnitPrice) {
}
