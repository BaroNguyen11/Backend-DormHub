package com.dormhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dormhub.dto.request.RoomMonthlyChargeInput;
import com.dormhub.dto.response.RoomMonthlyChargeResult;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class RoomMonthlyChargeCalculatorTest {

    private final RoomMonthlyChargeCalculator calculator = new RoomMonthlyChargeCalculator();

    @Test
    void calculatesUsageAndRoundsEachAmountBeforeTotaling() {
        RoomMonthlyChargeResult result = calculator.calculate(new RoomMonthlyChargeInput(
                1L,
                YearMonth.of(2026, 10), true,
                decimal("10.10"), decimal("11.35"), decimal("3.6"),
                decimal("20.00"), decimal("20.50"), decimal("3")));

        assertEquals(YearMonth.of(2026, 10), result.billingMonth());
        assertEquals(1L, result.roomId());
        assertEquals(decimal("1.25"), result.electricityConsumption());
        assertEquals(decimal("5"), result.electricityAmount());
        assertEquals(decimal("0.50"), result.waterConsumption());
        assertEquals(decimal("2"), result.waterAmount());
        assertEquals(decimal("7"), result.totalAmount());
    }

    @Test
    void acceptsZeroUsage() {
        RoomMonthlyChargeInput input = new RoomMonthlyChargeInput(
                1L,
                YearMonth.of(2026, 10), true,
                decimal("10"), decimal("10"), decimal("4000"),
                decimal("5"), decimal("5"), decimal("10000"));

        RoomMonthlyChargeResult result = calculator.calculate(input);

        assertEquals(0, result.electricityConsumption().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.waterConsumption().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.totalAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void rejectsMissingReading() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(null));
    }

    @Test
    void rejectsUnconfirmedReading() {
        RoomMonthlyChargeInput input = new RoomMonthlyChargeInput(
                1L,
                YearMonth.of(2026, 10), false,
                decimal("1"), decimal("2"), decimal("4000"),
                decimal("1"), decimal("2"), decimal("10000"));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(input));
    }

    @Test
    void rejectsMissingCounterInsteadOfUsingZero() {
        RoomMonthlyChargeInput input = new RoomMonthlyChargeInput(
                1L,
                YearMonth.of(2026, 10), true,
                null, decimal("2"), decimal("4000"),
                decimal("1"), decimal("2"), decimal("10000"));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(input));
    }

    @Test
    void rejectsCounterDecrease() {
        RoomMonthlyChargeInput input = new RoomMonthlyChargeInput(
                1L,
                YearMonth.of(2026, 10), true,
                decimal("2"), decimal("1"), decimal("4000"),
                decimal("1"), decimal("2"), decimal("10000"));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(input));
    }

    @Test
    void rejectsNegativeCounter() {
        RoomMonthlyChargeInput input = new RoomMonthlyChargeInput(
                1L,
                YearMonth.of(2026, 10), true,
                decimal("1"), decimal("2"), decimal("4000"),
                decimal("-1"), decimal("2"), decimal("10000"));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(input));
    }

    @Test
    void rejectsMissingOrNonpositiveSnapshotPrice() {
        RoomMonthlyChargeInput missingPrice = new RoomMonthlyChargeInput(
                1L,
                YearMonth.of(2026, 10), true,
                decimal("1"), decimal("2"), null,
                decimal("1"), decimal("2"), decimal("10000"));
        RoomMonthlyChargeInput zeroPrice = new RoomMonthlyChargeInput(
                1L,
                YearMonth.of(2026, 10), true,
                decimal("1"), decimal("2"), decimal("4000"),
                decimal("1"), decimal("2"), BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(missingPrice));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(zeroPrice));
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
