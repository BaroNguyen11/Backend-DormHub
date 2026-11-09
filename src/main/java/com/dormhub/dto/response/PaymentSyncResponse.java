package com.dormhub.dto.response;

/**
 * Kết quả một lượt đối soát chủ động với SePay (POST /api/v1/student/payments/sync).
 *
 * @param enabled đối soát có được bật không (cần dormhub.sepay.api-key)
 * @param checked số giao dịch SePay gần nhất đã được đưa vào đối soát
 * @param matched số giao dịch khớp phiên thanh toán (payment PENDING → SUCCESS)
 * @param skipped 1 nếu lượt gọi bị throttle (gọi quá dày), ngược lại 0
 */
public record PaymentSyncResponse(boolean enabled, int checked, int matched, int skipped) {
}
