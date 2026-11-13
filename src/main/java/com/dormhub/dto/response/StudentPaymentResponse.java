package com.dormhub.dto.response;

import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentPaymentResponse {

    private final Long paymentId;
    private final Long invoiceId;
    private final InvoiceType invoiceType;
    private final PaymentStatus paymentStatus;
    private final InvoiceStatus invoiceStatus;
    private final BigDecimal expectedAmount;
    private final String currency;
    private final String paymentReference;
    private final String receivingBankCode;
    private final String receivingAccountNumber;
    private final String vietQrUrl;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant paidAt;
    private final boolean expired;
}
