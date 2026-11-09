package com.dormhub.repository;

import com.dormhub.entity.Payment;
import com.dormhub.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    List<Payment> findAllByInvoice_InvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    Optional<Payment> findFirstByInvoice_InvoiceIdAndStatusOrderByCreatedAtDesc(
            Long invoiceId, PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.paymentId = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);

    /**
     * Phiên thanh toán đang PENDING còn hiệu lực gần nhất của sinh viên
     * (hóa đơn DEPOSIT hoặc ROOM_FEE thuộc hợp đồng của chính sinh viên đó) —
     * dùng cho màn tổng quan sau login để chuyển hướng về trang thanh toán đang dở.
     */
    @Query("select payment from Payment payment "
            + "where payment.invoice.contract.student.user.id = :userId "
            + "and payment.status = :status "
            + "and payment.expiresAt > :now "
            + "order by payment.paymentId desc")
    Optional<Payment> findLatestPayableByStudentUser(
            @Param("userId") Long userId,
            @Param("status") PaymentStatus status,
            @Param("now") Instant now);
}
