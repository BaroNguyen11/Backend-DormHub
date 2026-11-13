package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.ManagerPaymentResponse;
import com.dormhub.entity.Payment;
import com.dormhub.entity.enums.PaymentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh sách phiên thanh toán toàn hệ thống — trước đây menu "Theo dõi thanh toán"
 * của quản lý gọi resource /payments nhưng backend không có endpoint nào nên 404.
 */
@RestController
@RequestMapping("/api/v1/manager/payments")
public class ManagerPaymentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentRepository paymentRepository;

    public ManagerPaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<ManagerPaymentResponse>>> search(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "paymentId")));
        Page<ManagerPaymentResponse> payments = paymentRepository.findManagerHistory(
                        status == null ? "" : status.name(), search.trim(), pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.success(
                "Get payments successfully", payments));
    }

    private ManagerPaymentResponse toResponse(Payment payment) {
        return new ManagerPaymentResponse(
                payment.getPaymentId(),
                payment.getInvoice().getInvoiceId(),
                payment.getInvoice().getInvoiceType(),
                payment.getPaymentReference(),
                payment.getExpectedAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getReceivingBankCode(),
                payment.getReceivingAccountNumber(),
                payment.getCreatedAt(),
                payment.getExpiresAt(),
                payment.getPaidAt());
    }
}
