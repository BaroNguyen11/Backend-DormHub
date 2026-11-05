package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.StudentDashboardResponse;
import com.dormhub.entity.User;
import com.dormhub.service.StudentDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/dashboard")
public class StudentDashboardController {

    private final StudentDashboardService service;

    public StudentDashboardController(StudentDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<StudentDashboardResponse>> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(service.getDashboard(user)));
    }
}
