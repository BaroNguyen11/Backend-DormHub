package com.dormhub.controller;

import com.dormhub.dto.request.InvoiceFilterRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.InvoicePageResponse;
import com.dormhub.dto.response.InvoiceResponse;
import com.dormhub.entity.User;
import com.dormhub.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hóa đơn của sinh viên — chỉ trả về hóa đơn gắn với hợp đồng của chính sinh viên
 * (P0: trước đây SV bị rơi vào /api/v1/invoices của Manager — lỗi IDOR toàn hệ thống).
 */
@RestController
@RequestMapping("/api/v1/student/invoices")
@RequiredArgsConstructor
public class StudentInvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<InvoicePageResponse>> getMyInvoices(
            @AuthenticationPrincipal User user,
            @Valid InvoiceFilterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy hóa đơn của bạn thành công",
                invoiceService.listMyInvoices(user.getId(), request.getMonth(), request.getPage(), request.getSize())));
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getMyInvoice(
            @AuthenticationPrincipal User user,
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết hóa đơn thành công",
                invoiceService.getMyInvoice(user.getId(), invoiceId)));
    }
}
