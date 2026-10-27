package com.dormhub.dto.response;

import java.util.List;

public record InvoicePageResponse(
        List<InvoiceSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
