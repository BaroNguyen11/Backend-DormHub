package com.dormhub.dto.response;

import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvoiceResponse {
    private final Long invoiceId;
    private final InvoiceType invoiceType;
    private final Long roomId;
    private final Long contractId;
    private final YearMonth billingMonth;
    private final LocalDateTime issueDate;
    private final LocalDateTime dueDate;
    private final InvoiceStatus status;
    private final boolean overdue;
    private final BigDecimal totalAmount;
    private final List<InvoiceDetailResponse> details;
}
