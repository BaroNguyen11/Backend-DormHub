package com.dormhub.controller;

import com.dormhub.dto.request.CreateContractRequest;
import com.dormhub.dto.request.UpdateContractRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.ContractResponse;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.service.ContractService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PostMapping
    public ResponseEntity<ApiResponse<ContractResponse>> create(
            @Valid @RequestBody CreateContractRequest request) {
        ContractResponse response = contractService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/contracts/" + response.getContractId()))
                .body(ApiResponse.success("Tạo hợp đồng thành công", response));
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

    @PutMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractResponse>> updatePending(
            @PathVariable @Positive Long contractId,
            @Valid @RequestBody UpdateContractRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hợp đồng thành công",
                contractService.updatePending(contractId, request)));
    }

    @PatchMapping("/{contractId}/cancel")
    public ResponseEntity<ApiResponse<ContractResponse>> cancelPending(
            @PathVariable @Positive Long contractId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Hủy hợp đồng thành công", contractService.cancelPending(contractId)));
    }
}
