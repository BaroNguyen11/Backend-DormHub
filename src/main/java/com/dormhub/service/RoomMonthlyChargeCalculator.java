package com.dormhub.service;

import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.enums.ReadingStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

@Component
public class RoomMonthlyChargeCalculator {

    public RoomMonthlyChargeResult calculate(Long roomId, YearMonth billingMonth, MeterReading reading) {
        if (reading == null) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Meter reading is required");
        }
        if (billingMonth == null) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Billing month is required");
        }
        if (roomId == null) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Room ID is required");
        }
        if (reading.getStatus() != ReadingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Meter reading must be confirmed");
        }

        validateCounters("electricity", reading.getElectricityOld(), reading.getElectricityNew());
        validateCounters("water", reading.getWaterOld(), reading.getWaterNew());
        validateUnitPrice("electricity", reading.getElectricityPrice());
        validateUnitPrice("water", reading.getWaterPrice());

        BigDecimal electricityUsage = reading.getElectricityNew().subtract(reading.getElectricityOld());
        BigDecimal waterUsage = reading.getWaterNew().subtract(reading.getWaterOld());
        BigDecimal electricityAmount = electricityUsage.multiply(reading.getElectricityPrice())
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal waterAmount = waterUsage.multiply(reading.getWaterPrice())
                .setScale(0, RoundingMode.HALF_UP);

        return new RoomMonthlyChargeResult(
                roomId,
                billingMonth,
                electricityUsage,
                waterUsage,
                electricityAmount,
                waterAmount,
                electricityAmount.add(waterAmount));
    }

    private void validateCounters(String kind, BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || newValue == null) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, kind + " counters are required");
        }
        if (oldValue.signum() < 0 || newValue.signum() < 0) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, kind + " counters must be nonnegative");
        }
        if (newValue.compareTo(oldValue) < 0) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, kind + " new counter must not be below old counter");
        }
    }

    private void validateUnitPrice(String kind, BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, kind + " confirmed unit price must be positive");
        }
    }
}
