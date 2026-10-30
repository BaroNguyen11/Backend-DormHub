package com.dormhub.controller;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.service.RoomAssignmentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/room-assignments")
public class ManagerRoomAssignmentController {

    private final RoomAssignmentService roomAssignmentService;

    @GetMapping
    public Page<RoomAssignmentResponse> getAll(
            @RequestParam(required = false) RoomAssignmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return roomAssignmentService.getAll(status, page, size);
    }

    @GetMapping("/{id}")
    public RoomAssignmentResponse getById(@PathVariable Long id) {
        return roomAssignmentService.getById(id);
    }

}
