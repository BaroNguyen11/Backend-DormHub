package com.dormhub.controller;

import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.service.RoomMonthlyChargeService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/room-monthly-charges")
public class RoomMonthlyChargeController {

    private final RoomMonthlyChargeService service;

    public RoomMonthlyChargeController(RoomMonthlyChargeService service) {
        this.service = service;
    }

    @GetMapping
    public RoomMonthlyChargeResult calculate(
            @RequestParam @Positive Long roomId,
            @RequestParam @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String month) {
        try {
            return service.calculate(roomId, YearMonth.parse(month));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("month must use YYYY-MM", exception);
        }
    }
}
