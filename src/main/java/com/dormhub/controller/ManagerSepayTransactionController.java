package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.SepayTransactionResponse;
import com.dormhub.entity.enums.SepayTransactionStatus;
import com.dormhub.service.SepayTransactionAdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            @RequestParam(defaultValue = "20") int size) {
        Page<SepayTransactionResponse> transactions =
                adminService.search(status, page, size);
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
}
