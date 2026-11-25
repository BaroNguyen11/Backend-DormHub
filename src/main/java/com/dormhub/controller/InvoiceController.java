package com.dormhub.controller;

import com.dormhub.dto.request.InvoiceCreateRequest;
import com.dormhub.dto.request.InvoiceFilterRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.InvoicePageResponse;
import com.dormhub.dto.response.InvoiceResponse;
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
    public InvoiceController(InvoiceService service) {
        this.service = service;
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

}
