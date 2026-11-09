package com.dormhub.dto.response;

import com.dormhub.entity.enums.SepayTransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Giao dịch SePay nhận từ webhook — dùng cho màn "Giao dịch SePay" của quản lý
 * để trả lời chính xác câu hỏi: "tiền đã chuyển rồi vì sao hóa đơn chưa PAID?".
 * Mỗi giao dịch không khớp đều kèm reviewReason nêu rõ lý do bị loại.
 */
@Getter
@AllArgsConstructor
public class SepayTransactionResponse {

    private final Long id;
    private final Long sepayTransactionId;
    private final Long paymentId;
    private final Long invoiceId;
    private final String paymentReference;
    private final SepayTransactionStatus status;
    private final String gateway;
    private final String accountNumber;
    private final String transferType;
    private final BigDecimal transferAmount;
    private final String code;
    private final String content;
    private final String bankReferenceCode;
    private final Instant transactionAt;
    private final Instant receivedAt;
    private final String reviewReason;
}
