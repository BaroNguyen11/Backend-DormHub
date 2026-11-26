package com.dormhub.controller;

import com.dormhub.dto.request.MaintenanceCreateRequest;
import com.dormhub.dto.request.MaintenanceStatusUpdateRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.MaintenanceResponse;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.MaintenancePriority;
import com.dormhub.entity.enums.MaintenanceStatus;
import com.dormhub.entity.enums.MaintenanceType;
import com.dormhub.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;


    @PostMapping("/student/maintenance")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> create(
            @Valid @RequestBody MaintenanceCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        MaintenanceResponse response = maintenanceService.create(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo yêu cầu bảo trì thành công", response));
    }

    @GetMapping("/student/maintenance")
    public ResponseEntity<ApiResponse<Page<MaintenanceResponse>>> getMyRequests(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<MaintenanceResponse> response = maintenanceService.getMyRequests(user.getId(), search, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu bảo trì thành công", response));
    }

    @GetMapping("/student/maintenance/{maintenanceId}")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> getMyRequest(
            @PathVariable Long maintenanceId,
            @AuthenticationPrincipal User user
    ) {
        MaintenanceResponse response = maintenanceService.getMyRequest(maintenanceId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết yêu cầu bảo trì thành công", response));
    }


    @GetMapping("/manager/maintenance")
    public ResponseEntity<ApiResponse<Page<MaintenanceResponse>>> getRequestsForManager(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) MaintenancePriority priority,
            @RequestParam(required = false) MaintenanceType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<MaintenanceResponse> response = maintenanceService.getRequestsForManager(status, priority, type, page, size, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu bảo trì cho quản lý thành công", response));
    }

    @GetMapping("/manager/maintenance/{maintenanceId}")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> getRequestDetailForManager(
            @PathVariable Long maintenanceId
    ) {
        MaintenanceResponse response = maintenanceService.getRequestDetailForManager(maintenanceId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết yêu cầu bảo trì thành công", response));
    }

    @PatchMapping("/manager/maintenance/{maintenanceId}/status")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> updateStatus(
            @PathVariable Long maintenanceId,
            @Valid @RequestBody MaintenanceStatusUpdateRequest request
    ) {
        MaintenanceResponse response = maintenanceService.updateStatus(maintenanceId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái bảo trì thành công", response));
    }
}
