package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.PaymentSyncResponse;
import com.dormhub.dto.response.StudentPaymentResponse;
import com.dormhub.dto.response.StudentPaymentHistoryResponse;
import com.dormhub.entity.enums.PaymentStatus;
import com.dormhub.entity.User;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.service.PaymentService;
import com.dormhub.service.SepayPaymentSyncService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
public class StudentPaymentController {

    private final PaymentService paymentService;
    private final SepayPaymentSyncService sepayPaymentSyncService;

    public StudentPaymentController(PaymentService paymentService,
                                    SepayPaymentSyncService sepayPaymentSyncService) {
        this.paymentService = paymentService;
        this.sepayPaymentSyncService = sepayPaymentSyncService;
    }

    @PostMapping("/invoices/{invoiceId}/payments")
    public ResponseEntity<ApiResponse<StudentPaymentResponse>> initiate(
            @PathVariable Long invoiceId, @AuthenticationPrincipal User currentUser) {
        PaymentService.InitiationResult result = paymentService.initiate(invoiceId, currentUser);
        if (result.isCreated()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create("/api/v1/student/payments/" + result.getPayment().getPaymentId()))
                    .body(ApiResponse.success(result.getPayment()));
        }
        return ResponseEntity.ok(ApiResponse.success(result.getPayment()));
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<ApiResponse<StudentPaymentResponse>> get(
            @PathVariable Long paymentId, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.get(paymentId, currentUser)));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<Page<StudentPaymentHistoryResponse>>> history(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.history(currentUser, status, search, page, size)));
    }

    /**
     * Đối soát bù với SePay: gọi khi sinh viên bấm "Kiểm tra lại" ở trang kết quả
     * thanh toán. Kéo các giao dịch gần nhất từ SePay về và ghép vào phiên thanh
     * toán PENDING (pending → success) theo đúng nghiệp vụ của webhook.
     * Lỗi SePay không bao giờ làm endpoint này thất bại.
     */
    @PostMapping("/payments/sync")
    public ResponseEntity<ApiResponse<PaymentSyncResponse>> sync(
            @RequestParam(name = "force", required = false, defaultValue = "false") boolean force,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.success(
                sepayPaymentSyncService.syncForUser(currentUser.getId(), force)));
    }
}
