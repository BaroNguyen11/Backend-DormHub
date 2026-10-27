package com.dormhub.dto.response;

import com.dormhub.entity.InvoiceType;
import com.dormhub.entity.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

public record InvoiceSummaryResponse(
        Long invoiceId,
        InvoiceType invoiceType,
        Long roomId,
        Long contractId,
        YearMonth billingMonth,
        LocalDateTime issueDate,
        LocalDateTime dueDate,
        InvoiceStatus status,
        boolean overdue,
        BigDecimal totalAmount) {
}
