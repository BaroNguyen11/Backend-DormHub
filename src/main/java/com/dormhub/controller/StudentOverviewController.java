package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.StudentOverviewResponse;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Payment;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.PaymentStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.PaymentRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.StudentRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Bước tiếp theo" của sinh viên ngay sau đăng nhập — FE gọi GET /student/overview
 * và điều hướng theo {@code redirect} để sinh viên không phải tự tìm lại màn hình:
 *
 * <ul>
 *   <li>Có phiên thanh toán PENDING còn hạn → trang kết quả thanh toán
 *       (trang tự poll + tự đối soát SePay; tiền vào là tự SUCCESS).</li>
 *   <li>Đã có phòng (cọc đã xác nhận → RoomAssignment ASSIGNED) → "Phòng của tôi".</li>
 *   <li>Đơn đã duyệt, cọc chưa trả → chi tiết hóa đơn cọc.</li>
 *   <li>Còn lại → dashboard (tự hướng dẫn tìm phòng / theo dõi đơn).</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student")
public class StudentOverviewController {

    private final StudentRepository studentRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<StudentOverviewResponse>> overview(
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = currentUser.getId();

        Student student = studentRepository.findByUserId(userId).orElse(null);
        boolean hasRoom = student != null && roomAssignmentRepository.existsByStudent_IdAndStatusIn(
                student.getId(), List.of(RoomAssignmentStatus.ASSIGNED, RoomAssignmentStatus.CHECKED_IN));

        Payment pendingPayment = paymentRepository
                .findLatestPayableByStudentUser(userId, PaymentStatus.PENDING, Instant.now())
                .orElse(null);

        Invoice unpaidDeposit = invoiceRepository
                .findFirstByContract_Student_User_IdAndInvoiceTypeAndStatusOrderByInvoiceIdDesc(
                        userId, InvoiceType.DEPOSIT, InvoiceStatus.UNPAID)
                .orElse(null);

        Long pendingPaymentId = pendingPayment == null ? null : pendingPayment.getPaymentId();
        Long pendingInvoiceId = pendingPayment == null || pendingPayment.getInvoice() == null
                ? null : pendingPayment.getInvoice().getInvoiceId();

        final String redirect;
        if (pendingPaymentId != null) {
            redirect = "/student/payment-result?paymentId=" + pendingPaymentId
                    + "&invoiceId=" + pendingInvoiceId;
        } else if (hasRoom) {
            redirect = "/student";
        } else if (unpaidDeposit != null) {
            redirect = "/student/invoices/" + unpaidDeposit.getInvoiceId();
        } else {
            redirect = "/student";
        }

        return ResponseEntity.ok(ApiResponse.success(new StudentOverviewResponse(
                hasRoom,
                pendingPaymentId,
                pendingInvoiceId,
                unpaidDeposit == null ? null : unpaidDeposit.getInvoiceId(),
                redirect)));
    }
}
