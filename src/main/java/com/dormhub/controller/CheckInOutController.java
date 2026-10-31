package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.service.CheckInOutService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/manager/contracts")
@RequiredArgsConstructor
public class CheckInOutController {

    private final CheckInOutService checkInOutService;

    @PostMapping("/{contractId}/check-in")
    public ResponseEntity<ApiResponse<RoomAssignmentResponse>> checkIn(
            @PathVariable @Positive Long contractId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Check-in thành công", checkInOutService.checkIn(contractId)));
    }

    @PostMapping("/{contractId}/check-out")
    public ResponseEntity<ApiResponse<RoomAssignmentResponse>> checkOut(
            @PathVariable @Positive Long contractId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Check-out thành công", checkInOutService.checkOut(contractId)));
    }
}
