package com.dormhub.controller;

import com.dormhub.dto.request.RenewalCreateRequest;
import com.dormhub.dto.request.RenewalReviewRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.RenewalResponse;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.RenewalStatus;
import com.dormhub.service.RenewalService;
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
public class RenewalController {

    private final RenewalService renewalService;

    @PostMapping("/student/contracts/{contractId}/renewal-requests")
    public ResponseEntity<ApiResponse<RenewalResponse>> create(
            @PathVariable @Positive Long contractId,
            @Valid @RequestBody RenewalCreateRequest request,
            @AuthenticationPrincipal User user) {
        RenewalResponse response = renewalService.create(contractId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi yêu cầu gia hạn thành công", response));
    }

    @GetMapping("/student/renewal-requests")
    public ResponseEntity<ApiResponse<Page<RenewalResponse>>> getMyRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu gia hạn thành công",
                renewalService.getMyRequests(user.getId(), page, size)));
    }

    @GetMapping("/student/renewal-requests/{renewalRequestId}")
    public ResponseEntity<ApiResponse<RenewalResponse>> getMyRequest(
            @PathVariable @Positive Long renewalRequestId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Lấy yêu cầu gia hạn thành công",
                renewalService.getMyRequest(renewalRequestId, user.getId())));
    }

    @PatchMapping("/student/renewal-requests/{renewalRequestId}/cancel")
    public ResponseEntity<ApiResponse<RenewalResponse>> cancel(
            @PathVariable @Positive Long renewalRequestId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Hủy yêu cầu gia hạn thành công",
                renewalService.cancel(renewalRequestId, user.getId())));
    }

    @GetMapping("/manager/renewal-requests")
    public ResponseEntity<ApiResponse<Page<RenewalResponse>>> getRequestsForManager(
            @RequestParam(required = false) RenewalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu gia hạn thành công",
                renewalService.getRequestsForManager(status, page, size, search)));
    }

    @GetMapping("/manager/renewal-requests/{renewalRequestId}")
    public ResponseEntity<ApiResponse<RenewalResponse>> getRequestForManager(
            @PathVariable @Positive Long renewalRequestId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy yêu cầu gia hạn thành công",
                renewalService.getRequestForManager(renewalRequestId)));
    }

    @PatchMapping("/manager/renewal-requests/{renewalRequestId}/review")
    public ResponseEntity<ApiResponse<RenewalResponse>> review(
            @PathVariable @Positive Long renewalRequestId,
            @Valid @RequestBody RenewalReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Xử lý yêu cầu gia hạn thành công",
                renewalService.review(renewalRequestId, request, user.getId())));
    }
}
