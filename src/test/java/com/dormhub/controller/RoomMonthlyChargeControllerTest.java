package com.dormhub.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dormhub.entity.MeterReading;
import com.dormhub.entity.ReadingStatus;
import com.dormhub.entity.Room;
import com.dormhub.repository.MeterReadingRepository;
import com.dormhub.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomMonthlyChargeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Test
    void returnsConfirmedReadingCalculatedFromDatabase() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.CONFIRMED));

        mockMvc.perform(get("/api/v1/room-monthly-charges")
                        .param("roomId", room.getRoomId().toString())
                        .param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(room.getRoomId()))
                .andExpect(jsonPath("$.billingMonth").value("2026-10"))
                .andExpect(jsonPath("$.electricityConsumption").value(2.5))
                .andExpect(jsonPath("$.waterConsumption").value(1.5))
                .andExpect(jsonPath("$.electricityAmount").value(10000))
                .andExpect(jsonPath("$.waterAmount").value(15000))
                .andExpect(jsonPath("$.totalAmount").value(25000))
                .andExpect(jsonPath("$.electricityUnitPrice").doesNotExist());
    }

    @Test
    void returnsNotFoundWhenRoomOrReadingDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/room-monthly-charges")
                        .param("roomId", "999999")
                        .param("month", "2026-10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsConflictWhenReadingIsNotConfirmed() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.DRAFT));

        mockMvc.perform(get("/api/v1/room-monthly-charges")
                        .param("roomId", room.getRoomId().toString())
                        .param("month", "2026-10"))
                .andExpect(status().isConflict());
    }

    @Test
    void databaseRejectsConfirmedCountersThatDecrease() {
        Room room = roomRepository.saveAndFlush(new Room());
        assertThrows(DataIntegrityViolationException.class, () -> meterReadingRepository.saveAndFlush(
                new MeterReading(
                        room, YearMonth.of(2026, 10),
                        new BigDecimal("12.5"), new BigDecimal("10"),
                        new BigDecimal("3"), new BigDecimal("4.5"),
                        new BigDecimal("4000"), new BigDecimal("10000"),
                        ReadingStatus.CONFIRMED, LocalDateTime.of(2026, 10, 31, 12, 0))));
    }

    @Test
    void returnsBadRequestForInvalidMonth() throws Exception {
        mockMvc.perform(get("/api/v1/room-monthly-charges")
                        .param("roomId", "1")
                        .param("month", "2026-13"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestForNonpositiveRoomId() throws Exception {
        mockMvc.perform(get("/api/v1/room-monthly-charges")
                        .param("roomId", "0")
                        .param("month", "2026-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestForMissingQueryParameter() throws Exception {
        mockMvc.perform(get("/api/v1/room-monthly-charges").param("roomId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previousRouteIsNotRegistered() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/1/monthly-charge").param("month", "2026-10"))
                .andExpect(status().isNotFound());
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
