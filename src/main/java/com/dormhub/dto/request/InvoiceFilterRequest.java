package com.dormhub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class InvoiceFilterRequest {
    @jakarta.validation.constraints.Size(max = 200)
    private String search;

    private com.dormhub.entity.enums.InvoiceStatus status;

    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])")
    private String month;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 20;
}
