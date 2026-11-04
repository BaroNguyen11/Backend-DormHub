package com.dormhub.repository;

import com.dormhub.entity.Payment;
import com.dormhub.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
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
}
