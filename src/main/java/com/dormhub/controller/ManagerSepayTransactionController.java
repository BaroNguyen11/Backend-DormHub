package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.SepayTransactionResponse;
import com.dormhub.dto.request.SepayReviewRequest;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.SepayTransactionStatus;
import com.dormhub.service.SepayTransactionAdminService;
import org.springframework.data.domain.Page;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giao dịch SePay nhận qua webhook — màn chẩn đoán cho quản lý.
 * Nằm dưới /api/v1/manager/** nên SecurityConfig đã yêu cầu role MANAGER sẵn.
 */
@RestController
@RequestMapping("/api/v1/manager/sepay-transactions")
public class ManagerSepayTransactionController {

    private final SepayTransactionAdminService adminService;

    public ManagerSepayTransactionController(SepayTransactionAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SepayTransactionResponse>>> search(
            @RequestParam(required = false) SepayTransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        Page<SepayTransactionResponse> transactions =
                adminService.search(status, page, size, search);
        return ResponseEntity.ok(ApiResponse.success(
                "Get sepay transactions successfully", transactions));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<SepayTransactionResponse>> retry(
            @PathVariable Long id) {
        SepayTransactionResponse transaction = adminService.retry(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã đối soát lại giao dịch", transaction));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SepayTransactionResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.get(id)));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<SepayTransactionResponse>> recordReview(
            @PathVariable Long id,
            @Valid @RequestBody SepayReviewRequest request,
            @AuthenticationPrincipal User manager) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã ghi chú rà soát; chưa xác nhận thanh toán hoặc hoàn tiền",
                adminService.recordReview(id, manager.getId(), request.getNote())));
    }
}
