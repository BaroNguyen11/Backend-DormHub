package com.dormhub.dto.response;

import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record StudentPaymentHistoryResponse(
        Long paymentId,
        Long invoiceId,
        InvoiceType invoiceType,
        String paymentReference,
        BigDecimal expectedAmount,
        PaymentStatus paymentStatus,
        InvoiceStatus invoiceStatus,
        Instant createdAt,
        Instant expiresAt,
        Instant paidAt,
        boolean expired) {
}
