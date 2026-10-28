package com.dormhub.controller;

import com.dormhub.dto.request.InvoiceCreateRequest;
import com.dormhub.dto.response.InvoicePageResponse;
import com.dormhub.dto.response.InvoiceResponse;
import com.dormhub.service.InvoiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody InvoiceCreateRequest request) {
        if ("DEPOSIT".equalsIgnoreCase(request.getInvoiceType().trim())) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_IMPLEMENTED, "DEPOSIT invoice creation is not supported in this phase"));
        }
        InvoiceResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/invoices/" + response.invoiceId())).body(response);
    }

    @GetMapping
    public InvoicePageResponse list(
            @RequestParam(required = false)
            @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String month,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(month, page, size);
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }
}
