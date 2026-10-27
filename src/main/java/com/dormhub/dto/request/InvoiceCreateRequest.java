package com.dormhub.dto.request;

public record InvoiceCreateRequest(
        String invoiceType,
        Long roomId,
        Long contractId,
        String billingMonth) {
}
