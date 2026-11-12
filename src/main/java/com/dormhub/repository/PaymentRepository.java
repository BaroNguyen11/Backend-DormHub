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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    List<Payment> findAllByInvoice_InvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    @Query(value = """
            select p.* from payments p
            join invoices i on i.invoice_id = p.invoice_id
            left join contracts c on c.contract_id = i.contract_id
            left join students owner on owner.id = c.student_id
            where (:status = '' or p.status = :status)
              and ((i.invoice_type in ('ROOM_FEE', 'DEPOSIT') and owner.user_id = :userId)
                or (i.invoice_type = 'UTILITY' and i.room_id is not null
                  and i.billing_month is not null and exists (
                    select 1 from room_assignments a
                    join students resident on resident.id = a.student_id
                    where a.room_id = i.room_id and resident.user_id = :userId
                      and a.check_in_at is not null
                      and a.check_in_at < i.billing_month + interval '1 month'
                      and (a.check_out_at is null or a.check_out_at > i.billing_month))))
            order by p.created_at desc, p.payment_id desc
            """,
            countQuery = """
            select count(*) from payments p
            join invoices i on i.invoice_id = p.invoice_id
            left join contracts c on c.contract_id = i.contract_id
            left join students owner on owner.id = c.student_id
            where (:status = '' or p.status = :status)
              and ((i.invoice_type in ('ROOM_FEE', 'DEPOSIT') and owner.user_id = :userId)
                or (i.invoice_type = 'UTILITY' and i.room_id is not null
                  and i.billing_month is not null and exists (
                    select 1 from room_assignments a
                    join students resident on resident.id = a.student_id
                    where a.room_id = i.room_id and resident.user_id = :userId
                      and a.check_in_at is not null
                      and a.check_in_at < i.billing_month + interval '1 month'
                      and (a.check_out_at is null or a.check_out_at > i.billing_month))))
            """, nativeQuery = true)
    Page<Payment> findHistoryForStudent(@Param("userId") Long userId,
            @Param("status") String status, Pageable pageable);

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
