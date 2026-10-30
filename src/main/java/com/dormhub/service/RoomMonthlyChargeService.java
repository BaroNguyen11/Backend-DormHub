package com.dormhub.service;

import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.enums.ReadingStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.MeterReadingRepository;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomMonthlyChargeService {

    private final MeterReadingRepository meterReadingRepository;
    private final RoomMonthlyChargeCalculator calculator;

    public RoomMonthlyChargeService(
            MeterReadingRepository meterReadingRepository,
            RoomMonthlyChargeCalculator calculator) {
        this.meterReadingRepository = meterReadingRepository;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public RoomMonthlyChargeResult calculate(Long roomId, String month) {
        if (roomId == null || roomId <= 0 || month == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "A positive room ID and billing month are required");
        }
        YearMonth billingMonth;
        try {
            billingMonth = YearMonth.parse(month);
        } catch (DateTimeParseException exception) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "month must use YYYY-MM");
        }

        MeterReading reading = meterReadingRepository
                .findByRoom_IdAndReadingMonth(roomId, billingMonth);

        if (reading == null) {
            throw new AppException(ErrorCode.BILLING_NOT_FOUND,
                    "No meter reading for room " + roomId + " in " + billingMonth);
        }

        if (reading.getStatus() != ReadingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Meter reading is not confirmed");
        }

        return calculator.calculate(roomId, billingMonth, reading);
    }
}
