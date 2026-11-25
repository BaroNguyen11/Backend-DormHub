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
 * ROOM_FEE/DEPOSIT thuộc hợp đồng của sinh viên; UTILITY thuộc phòng và kỳ đã cư trú thực tế.
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
                invoiceService.listMyInvoices(user.getId(), request.getMonth(), request.getPage(), request.getSize(), request.getSearch(), request.getStatus())));
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
