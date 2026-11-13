package com.dormhub.controller;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.dto.response.RoommateResponse;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.entity.User;
import com.dormhub.service.RoomAssignmentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/room-assignments")
public class StudentRoomAssignmentController {

    private final RoomAssignmentService roomAssignmentService;

    @GetMapping("/roommates")
    public ResponseEntity<ApiResponse<List<RoommateResponse>>> getMyRoommates(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(roomAssignmentService.getMyRoommates(user.getId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomAssignmentResponse>>> getMyAssignments(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                roomAssignmentService.getMyAssignments(user.getId(), page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomAssignmentResponse>> getMyAssignment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                roomAssignmentService.getMyAssignment(user.getId(), id)));
    }
}
