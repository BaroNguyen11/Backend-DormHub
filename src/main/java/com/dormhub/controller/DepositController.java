package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.DepositFlowResponse;
import com.dormhub.service.DepositService;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @deprecated Deposit invoices are now auto-created when dorm applications are approved.
 * This controller is no longer needed in the standard workflow.
 * See: DormApplicationService.approve() which creates deposit invoices automatically.
 */
@Deprecated
@Validated
@RestController
@RequestMapping("/api/v1/manager")
public class DepositController {

    private final DepositService depositService;

    public DepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    /**
     * @deprecated Use automatic deposit invoice creation via application approval instead.
     */
    @Deprecated
    @PostMapping("/contracts/{contractId}/deposit-invoice")
    public ResponseEntity<ApiResponse<DepositFlowResponse>> createDepositInvoice(
            @PathVariable @Positive Long contractId) {
        DepositFlowResponse response = depositService.createDepositInvoice(contractId);
        return ResponseEntity.created(URI.create("/api/v1/invoices/" + response.getInvoiceId()))
                .body(ApiResponse.success("Tạo hóa đơn đặt cọc thành công", response));
    }
}
