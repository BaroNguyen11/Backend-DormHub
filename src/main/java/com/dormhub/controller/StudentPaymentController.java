package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.InvoicePageResponse;
import com.dormhub.dto.response.InvoiceResponse;
import com.dormhub.dto.response.StudentPaymentResponse;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.service.InvoiceService;
import com.dormhub.service.PaymentService;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;
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
    private final InvoiceService invoiceService;

    public StudentPaymentController(PaymentService paymentService, InvoiceService invoiceService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
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

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<InvoicePageResponse>> getMyInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        InvoiceStatus invoiceStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                invoiceStatus = InvoiceStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "status must be UNPAID or PAID");
            }
        }
        InvoicePageResponse response = invoiceService.listByStudent(currentUser.getId(), invoiceStatus, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getMyInvoice(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal User currentUser) {
        InvoiceResponse response = invoiceService.getByStudent(invoiceId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
