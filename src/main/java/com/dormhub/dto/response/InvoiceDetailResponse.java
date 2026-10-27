package com.dormhub.dto.response;

import com.dormhub.entity.InvoiceItemType;
import java.math.BigDecimal;

public record InvoiceDetailResponse(
        Long detailId,
        InvoiceItemType itemType,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount) {
}
