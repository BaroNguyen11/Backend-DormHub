package com.dormhub.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.PriceType;
import com.dormhub.entity.Prices;
import com.dormhub.entity.ReadingStatus;
import com.dormhub.entity.Room;
import com.dormhub.exception.RoomMonthlyChargeNotFoundException;
import com.dormhub.exception.RoomMonthlyChargeUnavailableException;
import com.dormhub.repository.MeterReadingRepository;
import com.dormhub.repository.PricesRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.service.RoomMonthlyChargeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RoomMonthlyChargeServiceIntegrationTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private PricesRepository pricesRepository;

    @Autowired
    private RoomMonthlyChargeService service;

    @Test
    void readsConfirmedSnapshotFromJpaAndCalculatesCharge() {
        Room room = roomRepository.saveAndFlush(new Room());
        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.CONFIRMED));

        RoomMonthlyChargeResult result = service.calculate(room.getRoomId(), YearMonth.of(2026, 10));

        assertEquals(room.getRoomId(), result.roomId());
        assertEquals(new BigDecimal("2.5"), result.electricityConsumption());
        assertEquals(new BigDecimal("10000"), result.electricityAmount());
        assertEquals(new BigDecimal("1.5"), result.waterConsumption());
        assertEquals(new BigDecimal("15000"), result.waterAmount());
        assertEquals(new BigDecimal("25000"), result.totalAmount());
    }

    @Test
    void rejectsMissingReading() {
        Room room = roomRepository.saveAndFlush(new Room());

        assertThrows(RoomMonthlyChargeNotFoundException.class,
                () -> service.calculate(room.getRoomId(), YearMonth.of(2026, 10)));
    }

    @Test
    void rejectsDraftReading() {
        Room room = roomRepository.saveAndFlush(new Room());
        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.DRAFT));

        assertThrows(RoomMonthlyChargeUnavailableException.class,
                () -> service.calculate(room.getRoomId(), YearMonth.of(2026, 10)));
    }

    @Test
    void usesConfirmedSnapshotInsteadOfCurrentPrice() {
        Room room = roomRepository.saveAndFlush(new Room());
        pricesRepository.saveAndFlush(new Prices(
                "water-rate-next-month", PriceType.WATER, new BigDecimal("20000"),
                LocalDate.of(2026, 11, 1), LocalDateTime.of(2026, 10, 31, 12, 0),
                "manager", null));
        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.CONFIRMED));

        RoomMonthlyChargeResult result = service.calculate(room.getRoomId(), YearMonth.of(2026, 10));

        assertEquals(new BigDecimal("15000"), result.waterAmount());
    }

    @Test
    void rejectsInvalidRoomIdBeforeRepositoryLookup() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(0L, YearMonth.of(2026, 10)));
    }

    private MeterReading reading(Room room, ReadingStatus status) {
        return new MeterReading(
                room,
                YearMonth.of(2026, 10),
                new BigDecimal("10"),
                new BigDecimal("12.5"),
                new BigDecimal("3"),
                new BigDecimal("4.5"),
                new BigDecimal("4000"),
                new BigDecimal("10000"),
                status,
                LocalDateTime.of(2026, 10, 31, 12, 0));
    }
}
