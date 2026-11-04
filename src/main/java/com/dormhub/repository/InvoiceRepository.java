package com.dormhub.repository;

import com.dormhub.entity.Invoice;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Override
    @EntityGraph(attributePaths = {"room", "contract", "contract.room"})
    Page<Invoice> findAll(Pageable pageable);

    boolean existsByRoom_IdAndBillingMonth(Long roomId, YearMonth billingMonth);

    boolean existsByContract_ContractIdAndBillingMonth(Long contractId, YearMonth billingMonth);

    boolean existsByContract_ContractIdAndInvoiceType(Long contractId, InvoiceType invoiceType);

    boolean existsByContract_ContractIdAndStatus(Long contractId, InvoiceStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invoice from Invoice invoice where invoice.invoiceId = :invoiceId")
    Optional<Invoice> findByIdForUpdate(@Param("invoiceId") Long invoiceId);

    @EntityGraph(attributePaths = {"room", "contract", "contract.room"})
    Page<Invoice> findByBillingMonth(YearMonth billingMonth, Pageable pageable);
    
    @EntityGraph(attributePaths = {"contract", "contract.student", "contract.student.user"})
    List<Invoice> findByStatusAndDueDateBetween(InvoiceStatus status, LocalDateTime start, LocalDateTime end);
}
