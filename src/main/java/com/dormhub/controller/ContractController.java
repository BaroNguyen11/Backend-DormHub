package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.ContractResponse;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.service.ContractService;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractResponse>> getById(
            @PathVariable @Positive Long contractId) {
        return ResponseEntity.ok(ApiResponse.success(contractService.getById(contractId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContractResponse>>> search(
            @RequestParam(required = false) @Positive Long studentId,
            @RequestParam(required = false) ContractStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                contractService.search(studentId, status, pageable)));
    }

    @PatchMapping("/{contractId}/cancel")
    public ResponseEntity<ApiResponse<ContractResponse>> cancelPending(
            @PathVariable @Positive Long contractId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Hủy hợp đồng thành công", contractService.cancelPending(contractId)));
    }
}
