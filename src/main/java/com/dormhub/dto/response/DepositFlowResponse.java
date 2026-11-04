package com.dormhub.dto.response;

import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DepositFlowResponse {

    private final Long contractId;
    private final Long invoiceId;
    private final InvoiceStatus invoiceStatus;
    private final ContractStatus contractStatus;
    private final DepositStatus depositStatus;
    private final BigDecimal depositAmount;
    private final LocalDateTime dueDate;
}
