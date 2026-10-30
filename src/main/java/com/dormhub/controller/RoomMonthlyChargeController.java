package com.dormhub.controller;

import com.dormhub.dto.request.RoomMonthlyChargeRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.service.RoomMonthlyChargeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/room-monthly-charges")
public class RoomMonthlyChargeController {

    private final RoomMonthlyChargeService service;

    public RoomMonthlyChargeController(RoomMonthlyChargeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomMonthlyChargeResult>> calculate(
            @Valid @RequestBody RoomMonthlyChargeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.calculate(request.getRoomId(), request.getMonth())));
    }
}
