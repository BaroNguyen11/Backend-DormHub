package com.dormhub.controller;

import com.dormhub.dto.request.DormApplicationRequest;
import com.dormhub.dto.request.DormApplicationRoomChangeRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.DormApplicationResponse;
import com.dormhub.entity.User;
import com.dormhub.service.DormApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/dorm-applications")
public class DormApplicationController {

    private final DormApplicationService dormApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<DormApplicationResponse>> submit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DormApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Nộp đơn đăng ký thành công", dormApplicationService.submit(user.getId(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DormApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(
                dormApplicationService.getMyApplications(user.getId(), page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DormApplicationResponse>> getMyApplication(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                dormApplicationService.getMyApplication(user.getId(), id)));
    }

    @PatchMapping("/{id}/room")
    public ResponseEntity<ApiResponse<DormApplicationResponse>> changeRequestedRoom(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody DormApplicationRoomChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng đã chọn thành công",
                dormApplicationService.changeRequestedRoom(user.getId(), id, request.getRoomId())));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<DormApplicationResponse>> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn đăng ký thành công",
                dormApplicationService.cancel(user.getId(), id)));
    }
}
