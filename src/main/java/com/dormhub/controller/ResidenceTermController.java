package com.dormhub.controller;

import com.dormhub.dto.request.ResidenceTermRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.ResidenceTermResponse;
import com.dormhub.entity.enums.ResidenceTermStatus;
import com.dormhub.service.ResidenceTermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ResidenceTermController {

    private final ResidenceTermService termService;

   
    @GetMapping("/residence-terms/active")
    public ResponseEntity<ApiResponse<ResidenceTermResponse>> getActiveTerm() {
        return ResponseEntity.ok(ApiResponse.success("Lấy đợt đăng ký đang mở thành công", termService.getActiveTerm()));
    }


    @GetMapping("/manager/residence-terms")
    public ResponseEntity<ApiResponse<Page<ResidenceTermResponse>>> getAllTerms(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đợt đăng ký thành công", termService.getAllTerms(pageable)));
    }


    @GetMapping("/manager/residence-terms/{id}")
    public ResponseEntity<ApiResponse<ResidenceTermResponse>> getTermById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đợt đăng ký thành công", termService.getTermById(id)));
    }

    @PostMapping("/manager/residence-terms")
    public ResponseEntity<ApiResponse<ResidenceTermResponse>> createTerm(@Valid @RequestBody ResidenceTermRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đợt đăng ký thành công", termService.createTerm(request)));
    }

    @PutMapping("/manager/residence-terms/{id}")
    public ResponseEntity<ApiResponse<ResidenceTermResponse>> updateTerm(
            @PathVariable Long id,
            @Valid @RequestBody ResidenceTermRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đợt đăng ký thành công", termService.updateTerm(id, request)));
    }

    @PatchMapping("/manager/residence-terms/{id}/status")
    public ResponseEntity<ApiResponse<ResidenceTermResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam ResidenceTermStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", termService.updateStatus(id, status)));
    }
}