package com.dormhub.controller;

import com.dormhub.dto.request.DormApplicationReviewRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.DormApplicationResponse;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.service.DormApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/dorm-applications")
public class ManagerDormApplicationController {

    private final DormApplicationService dormApplicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DormApplicationResponse>>> getAll(
            @RequestParam(required = false) DormApplicationStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(dormApplicationService.getAll(status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DormApplicationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(dormApplicationService.getById(id)));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<ApiResponse<DormApplicationResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody DormApplicationReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Xử lý đơn đăng ký thành công",
                dormApplicationService.review(id, request)));
    }

}
