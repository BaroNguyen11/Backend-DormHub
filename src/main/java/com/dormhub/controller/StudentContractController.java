package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.ContractResponse;
import com.dormhub.dto.response.CheckInPlanResponse;
import com.dormhub.dto.request.CheckInDateRequest;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.service.ContractService;
import com.dormhub.service.ContractPdfService;
import java.io.IOException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/student/contracts")
public class StudentContractController {

    private final ContractService contractService;
    private final ContractPdfService contractPdfService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContractResponse>>> getMyContracts(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                contractService.getMyContracts(user.getId(), status, pageRequest)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractResponse>> getMyContract(
            @AuthenticationPrincipal User user,
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.getMyContract(user.getId(), id)));
    }

    @GetMapping("/{id}/check-in-plan")
    public ResponseEntity<ApiResponse<CheckInPlanResponse>> previewCheckIn(
            @AuthenticationPrincipal User user, @PathVariable @Positive Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.previewCheckIn(user.getId(), id, date)));
    }

    @PostMapping("/{id}/confirm-check-in-date")
    public ResponseEntity<ApiResponse<ContractResponse>> confirmCheckIn(
            @AuthenticationPrincipal User user, @PathVariable @Positive Long id,
            @Valid @RequestBody CheckInDateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã xác nhận ngày nhận phòng",
                contractService.confirmCheckIn(user.getId(), id, request.getCheckInDate())));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getMyContractPdf(
            @AuthenticationPrincipal User user,
            @PathVariable @Positive Long id) throws IOException {
        byte[] pdf = contractPdfService.renderForStudent(user.getId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=contract-" + id + ".pdf")
                .cacheControl(CacheControl.noStore())
                .body(pdf);
    }
}

