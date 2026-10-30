package com.dormhub.dto.response;

import com.dormhub.entity.enums.InvoiceItemType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvoiceDetailResponse {
    private final Long detailId;
    private final InvoiceItemType itemType;
    private final String description;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal amount;
}
