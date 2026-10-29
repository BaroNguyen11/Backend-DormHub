package com.dormhub.service.impl;

import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.ReadingStatus;
import com.dormhub.exception.RoomMonthlyChargeNotFoundException;
import com.dormhub.exception.RoomMonthlyChargeUnavailableException;
import com.dormhub.repository.MeterReadingRepository;
import com.dormhub.service.RoomMonthlyChargeCalculator;
import com.dormhub.service.RoomMonthlyChargeService;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomMonthlyChargeServiceImpl implements RoomMonthlyChargeService {

    private final MeterReadingRepository meterReadingRepository;
    private final RoomMonthlyChargeCalculator calculator;

    public RoomMonthlyChargeServiceImpl(
            MeterReadingRepository meterReadingRepository,
            RoomMonthlyChargeCalculator calculator) {
        this.meterReadingRepository = meterReadingRepository;
        this.calculator = calculator;
    }

    @Override
    @Transactional(readOnly = true)
    public RoomMonthlyChargeResult calculate(Long roomId, String month) {
        if (roomId == null || roomId <= 0 || month == null) {
            throw new IllegalArgumentException("A positive room ID and billing month are required");
        }
        YearMonth billingMonth;
        try {
            billingMonth = YearMonth.parse(month);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("month must use YYYY-MM", exception);
        }

        MeterReading reading = meterReadingRepository
                .findByRoom_RoomIdAndReadingMonth(roomId, billingMonth);

        if (reading == null) {
            throw new RoomMonthlyChargeNotFoundException(roomId, billingMonth);
        }

        if (reading.getStatus() != ReadingStatus.CONFIRMED) {
            throw new RoomMonthlyChargeUnavailableException("Meter reading is not confirmed");
        }

        try {
            return calculator.calculate(roomId, billingMonth, reading);
        } catch (IllegalArgumentException exception) {
            throw new RoomMonthlyChargeUnavailableException(exception.getMessage());
        }
    }
}
