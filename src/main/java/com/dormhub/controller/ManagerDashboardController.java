package com.dormhub.controller;

import com.dormhub.dto.request.ManagerDashboardRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.ManagerDashboardResponse;
import com.dormhub.service.ManagerDashboardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/dashboard")
public class ManagerDashboardController {

    private final ManagerDashboardService service;

    public ManagerDashboardController(ManagerDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ManagerDashboardResponse>> get(@Valid ManagerDashboardRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.getDashboard(request.getYear())));
    }
}
