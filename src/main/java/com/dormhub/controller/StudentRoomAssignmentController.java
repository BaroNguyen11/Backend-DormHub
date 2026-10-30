package com.dormhub.controller;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.entity.User;
import com.dormhub.service.RoomAssignmentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/room-assignments")
public class StudentRoomAssignmentController {

    private final RoomAssignmentService roomAssignmentService;

    @GetMapping
    public Page<RoomAssignmentResponse> getMyAssignments(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return roomAssignmentService.getMyAssignments(user.getId(), page, size);
    }

    @GetMapping("/{id}")
    public RoomAssignmentResponse getMyAssignment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return roomAssignmentService.getMyAssignment(user.getId(), id);
    }
}
