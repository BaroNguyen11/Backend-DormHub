package com.dormhub.controller;

import com.dormhub.dto.request.InvoiceCreateRequest;
import com.dormhub.dto.request.InvoiceFilterRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.DepositFlowResponse;
import com.dormhub.dto.response.InvoicePageResponse;
import com.dormhub.dto.response.InvoiceResponse;
import com.dormhub.service.DepositService;
import com.dormhub.service.InvoiceService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService service;
    private final DepositService depositService;

    public InvoiceController(InvoiceService service, DepositService depositService) {
        this.service = service;
        this.depositService = depositService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(@Valid @RequestBody InvoiceCreateRequest request) {
        InvoiceResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/invoices/" + response.getInvoiceId()))
                .body(ApiResponse.success("Tạo hóa đơn thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<InvoicePageResponse>> list(@Valid InvoiceFilterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.list(request.getMonth(), request.getPage(), request.getSize())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.get(id)));
    }

    /**
     * FIX luồng cọc: quản lý xác nhận đã nhận tiền cọc trực tiếp (tiền mặt / chuyển
     * khoản ngoài hệ thống) — đường chính thức khi chưa có webhook SePay.
     * Gọi DepositService.confirmDepositPaid: invoice PAID + hợp đồng ACTIVE + cọc HELD
     * + TỰ ĐỘNG PHÂN PHÒNG (RoomAssignmentService.assign) + sinh hóa đơn tháng đầu.
     * Endpoint nằm dưới /api/v1/invoices/** mà SecurityConfig đã chặn role MANAGER.
     */
    @PostMapping("/{id}/confirm-cash-deposit")
    public ResponseEntity<ApiResponse<DepositFlowResponse>> confirmCashDeposit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã xác nhận thanh toán cọc và phân phòng",
                depositService.confirmDepositPaid(id)));
    }
}
