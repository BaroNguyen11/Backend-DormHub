package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.StudentPaymentResponse;
import com.dormhub.entity.User;
import com.dormhub.service.PaymentService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
public class StudentPaymentController {

    private final PaymentService paymentService;

    public StudentPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
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
}
