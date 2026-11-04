package com.dormhub.controller;

import com.dormhub.dto.request.RoomTransferCreateRequest;
import com.dormhub.dto.request.RoomTransferReviewRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.RoomTransferResponse;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.TransferStatus;
import com.dormhub.service.RoomTransferService;
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
public class RoomTransferController {

    private final RoomTransferService roomTransferService;

    @PostMapping("/student/room-transfer-requests")
    public ResponseEntity<ApiResponse<RoomTransferResponse>> create(
            @Valid @RequestBody RoomTransferCreateRequest request,
            @AuthenticationPrincipal User user) {
        RoomTransferResponse response = roomTransferService.create(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi yêu cầu chuyển phòng thành công", response));
    }

    @GetMapping("/student/room-transfer-requests")
    public ResponseEntity<ApiResponse<Page<RoomTransferResponse>>> getMyRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu chuyển phòng thành công",
                roomTransferService.getMyRequests(user.getId(), page, size)));
    }

    @GetMapping("/student/room-transfer-requests/{transferId}")
    public ResponseEntity<ApiResponse<RoomTransferResponse>> getMyRequest(
            @PathVariable @Positive Long transferId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Lấy yêu cầu chuyển phòng thành công",
                roomTransferService.getMyRequest(transferId, user.getId())));
    }

    @PatchMapping("/student/room-transfer-requests/{transferId}/cancel")
    public ResponseEntity<ApiResponse<RoomTransferResponse>> cancel(
            @PathVariable @Positive Long transferId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Hủy yêu cầu chuyển phòng thành công",
                roomTransferService.cancel(transferId, user.getId())));
    }

    @GetMapping("/manager/room-transfer-requests")
    public ResponseEntity<ApiResponse<Page<RoomTransferResponse>>> getRequestsForManager(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu chuyển phòng thành công",
                roomTransferService.getRequestsForManager(status, page, size)));
    }

    @GetMapping("/manager/room-transfer-requests/{transferId}")
    public ResponseEntity<ApiResponse<RoomTransferResponse>> getRequestForManager(
            @PathVariable @Positive Long transferId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy yêu cầu chuyển phòng thành công",
                roomTransferService.getRequestForManager(transferId)));
    }

    @PatchMapping("/manager/room-transfer-requests/{transferId}/review")
    public ResponseEntity<ApiResponse<RoomTransferResponse>> review(
            @PathVariable @Positive Long transferId,
            @Valid @RequestBody RoomTransferReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Xử lý yêu cầu chuyển phòng thành công",
                roomTransferService.review(transferId, request, user.getId())));
    }
}
