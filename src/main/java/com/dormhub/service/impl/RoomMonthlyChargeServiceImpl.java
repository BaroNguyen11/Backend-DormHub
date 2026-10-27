package com.dormhub.service.impl;

import com.dormhub.dto.request.RoomMonthlyChargeInput;
import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.ReadingStatus;
import com.dormhub.exception.RoomMonthlyChargeNotFoundException;
import com.dormhub.exception.RoomMonthlyChargeUnavailableException;
import com.dormhub.repository.MeterReadingRepository;
import com.dormhub.service.RoomMonthlyChargeCalculator;
import com.dormhub.service.RoomMonthlyChargeService;
import java.time.YearMonth;
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
    public RoomMonthlyChargeResult calculate(Long roomId, YearMonth billingMonth) {
        if (roomId == null || roomId <= 0 || billingMonth == null) {
            throw new IllegalArgumentException("A positive room ID and billing month are required");
        }

        MeterReading reading = meterReadingRepository
                .findByRoom_RoomIdAndReadingMonth(roomId, billingMonth)
                .orElseThrow(() -> new RoomMonthlyChargeNotFoundException(roomId, billingMonth));

        if (reading.getStatus() != ReadingStatus.CONFIRMED) {
            throw new RoomMonthlyChargeUnavailableException("Meter reading is not confirmed");
        }

        try {
            return calculator.calculate(new RoomMonthlyChargeInput(
                    roomId,
                    billingMonth,
                    true,
                    reading.getElectricityOld(),
                    reading.getElectricityNew(),
                    reading.getElectricityPrice(),
                    reading.getWaterOld(),
                    reading.getWaterNew(),
                    reading.getWaterPrice()));
        } catch (IllegalArgumentException exception) {
            throw new RoomMonthlyChargeUnavailableException(exception.getMessage());
        }
    }
}
