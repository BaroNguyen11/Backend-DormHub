
package com.dormhub.controller;

import com.dormhub.dto.request.MaintenanceCreateRequest;
import com.dormhub.dto.request.MaintenanceStatusUpdateRequest;
import com.dormhub.dto.response.MaintenanceResponse;
import com.dormhub.entity.User;
import com.dormhub.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    // Sinh viên tạo yêu cầu bảo trì
    @PostMapping("/student/maintenance")
    public ResponseEntity<MaintenanceResponse> create(@Valid @RequestBody MaintenanceCreateRequest request, @AuthenticationPrincipal User user) {
        MaintenanceResponse response = maintenanceService.create(request, user.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Sinh viên xem danh sách yêu cầu của mình
    @GetMapping("/student/maintenance")
    public ResponseEntity<List<MaintenanceResponse>> getMyRequests(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                maintenanceService.getMyRequests(user.getId())
        );
    }

    // Sinh viên xem chi tiết yêu cầu của mình
    @GetMapping("/student/maintenance/{maintenanceId}")
    public ResponseEntity<MaintenanceResponse> getMyRequest(@PathVariable Long maintenanceId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                maintenanceService.getMyRequest(
                        maintenanceId,
                        user.getId()
                )
        );
    }

    // Manager cập nhật trạng thái yêu cầu
    @PatchMapping("/manager/maintenance/{maintenanceId}/status")
    public ResponseEntity<MaintenanceResponse> updateStatus(@PathVariable Long maintenanceId, @Valid @RequestBody MaintenanceStatusUpdateRequest request) {
        return ResponseEntity.ok(
                maintenanceService.updateStatus(
                        maintenanceId,
                        request
                )
        );
    }
}
