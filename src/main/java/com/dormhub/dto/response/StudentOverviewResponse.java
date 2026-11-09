package com.dormhub.dto.response;

/**
 * Trạng thái tổng quan của sinh viên ngay sau đăng nhập — FE dùng trường
 * {@code redirect} để tự chuyển hướng tới đúng màn hình:
 *
 * <ul>
 *   <li>Có phiên thanh toán đang chờ → {@code /student/payment-result?paymentId=…}
 *       (trang này tự poll + tự đối soát SePay, tiền vào là tự xanh).</li>
 *   <li>Đã có phòng (cọc đã xác nhận, hợp đồng ACTIVE) → {@code /student}
 *       (màn "Phòng của tôi").</li>
 *   <li>Có hóa đơn cọc chưa trả → chi tiết hóa đơn đó để thanh toán cọc.</li>
 *   <li>Còn lại → {@code /student} (dashboard tự hướng dẫn tìm phòng/đơn đăng ký).</li>
 * </ul>
 */
public record StudentOverviewResponse(
        boolean hasRoom,
        Long pendingPaymentId,
        Long pendingInvoiceId,
        Long unpaidDepositInvoiceId,
        String redirect) {
}
