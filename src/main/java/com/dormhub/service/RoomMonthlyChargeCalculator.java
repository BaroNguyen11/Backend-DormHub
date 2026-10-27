package com.dormhub.service;

import com.dormhub.dto.request.RoomMonthlyChargeInput;
import com.dormhub.dto.response.RoomMonthlyChargeResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class RoomMonthlyChargeCalculator {

    public RoomMonthlyChargeResult calculate(RoomMonthlyChargeInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Meter reading is required");
        }
        if (input.billingMonth() == null) {
            throw new IllegalArgumentException("Billing month is required");
        }
        if (input.roomId() == null) {
            throw new IllegalArgumentException("Room ID is required");
        }
        if (!input.confirmed()) {
            throw new IllegalArgumentException("Meter reading must be confirmed");
        }

        validateCounters("electricity", input.electricityOld(), input.electricityNew());
        validateCounters("water", input.waterOld(), input.waterNew());
        validateUnitPrice("electricity", input.electricityUnitPrice());
        validateUnitPrice("water", input.waterUnitPrice());

        BigDecimal electricityUsage = input.electricityNew().subtract(input.electricityOld());
        BigDecimal waterUsage = input.waterNew().subtract(input.waterOld());
        BigDecimal electricityAmount = electricityUsage.multiply(input.electricityUnitPrice())
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal waterAmount = waterUsage.multiply(input.waterUnitPrice())
                .setScale(0, RoundingMode.HALF_UP);

        return new RoomMonthlyChargeResult(
                input.roomId(),
                input.billingMonth(),
                electricityUsage,
                waterUsage,
                electricityAmount,
                waterAmount,
                electricityAmount.add(waterAmount));
    }

    private void validateCounters(String kind, BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || newValue == null) {
            throw new IllegalArgumentException(kind + " counters are required");
        }
        if (oldValue.signum() < 0 || newValue.signum() < 0) {
            throw new IllegalArgumentException(kind + " counters must be nonnegative");
        }
        if (newValue.compareTo(oldValue) < 0) {
            throw new IllegalArgumentException(kind + " new counter must not be below old counter");
        }
    }

    private void validateUnitPrice(String kind, BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException(kind + " confirmed unit price must be positive");
        }
    }
}
