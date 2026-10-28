package com.dormhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.ReadingStatus;
import com.dormhub.entity.Room;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class RoomMonthlyChargeCalculatorTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 10);
    private final RoomMonthlyChargeCalculator calculator = new RoomMonthlyChargeCalculator();

    @Test
    void calculatesUsageAndRoundsEachAmountBeforeTotaling() {
        RoomMonthlyChargeResult result = calculator.calculate(1L, MONTH, reading(
                ReadingStatus.CONFIRMED,
                decimal("10.10"), decimal("11.35"), decimal("3.6"),
                decimal("20.00"), decimal("20.50"), decimal("3")));

        assertEquals(MONTH, result.billingMonth());
        assertEquals(1L, result.roomId());
        assertEquals(decimal("1.25"), result.electricityConsumption());
        assertEquals(decimal("5"), result.electricityAmount());
        assertEquals(decimal("0.50"), result.waterConsumption());
        assertEquals(decimal("2"), result.waterAmount());
        assertEquals(decimal("7"), result.totalAmount());
    }

    @Test
    void acceptsZeroUsage() {
        RoomMonthlyChargeResult result = calculator.calculate(1L, MONTH, reading(
                ReadingStatus.CONFIRMED,
                decimal("10"), decimal("10"), decimal("4000"),
                decimal("5"), decimal("5"), decimal("10000")));

        assertEquals(0, result.electricityConsumption().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.waterConsumption().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.totalAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void rejectsMissingReading() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(1L, MONTH, null));
    }

    @Test
    void rejectsUnconfirmedReading() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(1L, MONTH, reading(
                ReadingStatus.DRAFT,
                decimal("1"), decimal("2"), decimal("4000"),
                decimal("1"), decimal("2"), decimal("10000"))));
    }

    @Test
    void rejectsMissingCounterInsteadOfUsingZero() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(1L, MONTH, reading(
                ReadingStatus.CONFIRMED,
                null, decimal("2"), decimal("4000"),
                decimal("1"), decimal("2"), decimal("10000"))));
    }

    @Test
    void rejectsCounterDecrease() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(1L, MONTH, reading(
                ReadingStatus.CONFIRMED,
                decimal("2"), decimal("1"), decimal("4000"),
                decimal("1"), decimal("2"), decimal("10000"))));
    }

    @Test
    void rejectsNegativeCounter() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(1L, MONTH, reading(
                ReadingStatus.CONFIRMED,
                decimal("1"), decimal("2"), decimal("4000"),
                decimal("-1"), decimal("2"), decimal("10000"))));
    }

    @Test
    void rejectsMissingOrNonpositiveSnapshotPrice() {
        MeterReading missingPrice = reading(ReadingStatus.CONFIRMED,
                decimal("1"), decimal("2"), null,
                decimal("1"), decimal("2"), decimal("10000"));
        MeterReading zeroPrice = reading(ReadingStatus.CONFIRMED,
                decimal("1"), decimal("2"), decimal("4000"),
                decimal("1"), decimal("2"), BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(1L, MONTH, missingPrice));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(1L, MONTH, zeroPrice));
    }

    private static MeterReading reading(ReadingStatus status,
            BigDecimal electricityOld, BigDecimal electricityNew, BigDecimal electricityPrice,
            BigDecimal waterOld, BigDecimal waterNew, BigDecimal waterPrice) {
        return new MeterReading(new Room(), MONTH,
                electricityOld, electricityNew, waterOld, waterNew,
                electricityPrice, waterPrice, status,
                LocalDateTime.of(2026, 10, 31, 12, 0));
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
