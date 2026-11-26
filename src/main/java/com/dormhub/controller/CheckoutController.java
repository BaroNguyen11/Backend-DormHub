package com.dormhub.controller;

import com.dormhub.dto.request.CheckoutCreateRequest;
import com.dormhub.dto.request.CheckoutCompleteRequest;
import com.dormhub.dto.request.CheckoutReviewRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.CheckoutResponse;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.CheckoutRequestStatus;
import com.dormhub.service.CheckoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/student/checkout-requests")
    public ResponseEntity<ApiResponse<CheckoutResponse>> create(
            @Valid @RequestBody CheckoutCreateRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi yêu cầu trả phòng thành công",
                        checkoutService.create(user.getId(), request)));
    }

    @GetMapping("/student/checkout-requests")
    public ResponseEntity<ApiResponse<Page<CheckoutResponse>>> getMyRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) CheckoutRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu trả phòng thành công",
                checkoutService.getMyRequests(user.getId(), page, size, status)));
    }

    @GetMapping("/student/checkout-requests/{checkoutRequestId}")
    public ResponseEntity<ApiResponse<CheckoutResponse>> getMyRequest(
            @PathVariable @Positive Long checkoutRequestId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Lấy yêu cầu trả phòng thành công",
                checkoutService.getMyRequest(checkoutRequestId, user.getId())));
    }

    @PatchMapping("/student/checkout-requests/{checkoutRequestId}/cancel")
    public ResponseEntity<ApiResponse<CheckoutResponse>> cancel(
            @PathVariable @Positive Long checkoutRequestId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Hủy yêu cầu trả phòng thành công",
                checkoutService.cancel(checkoutRequestId, user.getId())));
    }

    @GetMapping("/manager/checkout-requests")
    public ResponseEntity<ApiResponse<Page<CheckoutResponse>>> getRequestsForManager(
            @RequestParam(required = false) CheckoutRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu trả phòng thành công",
                checkoutService.getRequestsForManager(status, page, size, search)));
    }

    @GetMapping("/manager/checkout-requests/{checkoutRequestId}")
    public ResponseEntity<ApiResponse<CheckoutResponse>> getRequestForManager(
            @PathVariable @Positive Long checkoutRequestId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy yêu cầu trả phòng thành công",
                checkoutService.getRequestForManager(checkoutRequestId)));
    }

    @PatchMapping("/manager/checkout-requests/{checkoutRequestId}/review")
    public ResponseEntity<ApiResponse<CheckoutResponse>> review(
            @PathVariable @Positive Long checkoutRequestId,
            @Valid @RequestBody CheckoutReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Xử lý yêu cầu trả phòng thành công",
                checkoutService.review(checkoutRequestId, request, user.getId())));
    }

    @PatchMapping("/manager/checkout-requests/{checkoutRequestId}/cancel")
    public ResponseEntity<ApiResponse<CheckoutResponse>> cancelForManager(
            @PathVariable @Positive Long checkoutRequestId,
            @Valid @RequestBody CheckoutReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Hủy yêu cầu trả phòng thành công",
                checkoutService.cancelForManager(checkoutRequestId, request, user.getId())));
    }

    @PostMapping("/manager/checkout-requests/{checkoutRequestId}/complete")
    public ResponseEntity<ApiResponse<CheckoutResponse>> complete(
            @PathVariable @Positive Long checkoutRequestId,
            @Valid @RequestBody CheckoutCompleteRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận trả phòng thành công",
                checkoutService.complete(checkoutRequestId, request, user.getId())));
    }
}
