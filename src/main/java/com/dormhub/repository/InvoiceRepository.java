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
    @EntityGraph(attributePaths = {"room", "room.building", "contract", "contract.room", "contract.room.building", "contract.student", "contract.student.user"})
    Page<Invoice> findAll(Pageable pageable);

    boolean existsByRoom_IdAndBillingMonth(Long roomId, YearMonth billingMonth);

    boolean existsByContract_ContractIdAndBillingMonth(Long contractId, YearMonth billingMonth);

    boolean existsByContract_ContractIdAndInvoiceType(Long contractId, InvoiceType invoiceType);

    boolean existsByContract_ContractIdAndStatus(Long contractId, InvoiceStatus status);

    boolean existsByRoom_IdAndStatusAndInvoiceType(Long roomId, InvoiceStatus status, InvoiceType invoiceType);

    Optional<Invoice> findByContract_ContractIdAndInvoiceType(Long contractId, InvoiceType invoiceType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invoice from Invoice invoice where invoice.contract.contractId = :contractId "
            + "and invoice.invoiceType = :invoiceType")
    Optional<Invoice> findByContract_ContractIdAndInvoiceTypeForUpdate(
            @Param("contractId") Long contractId,
            @Param("invoiceType") InvoiceType invoiceType);

    List<Invoice> findAllByContract_ContractIdInAndInvoiceType(
            List<Long> contractIds, InvoiceType invoiceType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invoice from Invoice invoice where invoice.invoiceId = :invoiceId")
    Optional<Invoice> findByIdForUpdate(@Param("invoiceId") Long invoiceId);

    @EntityGraph(attributePaths = {"room", "room.building", "contract", "contract.room", "contract.room.building", "contract.student", "contract.student.user"})
    Page<Invoice> findByBillingMonth(YearMonth billingMonth, Pageable pageable);
    
    @EntityGraph(attributePaths = {"room", "room.building", "contract", "contract.room", "contract.room.building", "contract.student", "contract.student.user"})
    List<Invoice> findByStatusAndDueDateBetween(InvoiceStatus status, LocalDateTime start, LocalDateTime end);

    // ===== Hóa đơn của sinh viên (chỉ của chính mình - kiểm soát sở hữu) =====

    @EntityGraph(attributePaths = {"room", "room.building", "contract", "contract.room", "contract.room.building", "contract.student", "contract.student.user"})
    Page<Invoice> findAllByContract_Student_User_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "room.building", "contract", "contract.room", "contract.room.building", "contract.student", "contract.student.user"})
    Page<Invoice> findAllByContract_Student_User_IdAndBillingMonth(
            Long userId, YearMonth billingMonth, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "room.building", "contract", "contract.room", "contract.room.building", "details"})
    Optional<Invoice> findByInvoiceIdAndContract_Student_User_Id(Long invoiceId, Long userId);

    // Hóa đơn đặt cọc CHƯA TRẢ gần nhất của sinh viên — dùng cho redirect sau login
    // (đơn đã duyệt nhưng chưa trả cọc -> đưa thẳng tới hóa đơn cần thanh toán).
    // Lưu ý: khóa chính của Invoice là invoiceId (cột invoice_id), KHÔNG phải "id".
    Optional<Invoice> findFirstByContract_Student_User_IdAndInvoiceTypeAndStatusOrderByInvoiceIdDesc(
            Long userId, InvoiceType invoiceType, InvoiceStatus status);
    
    long countByStatus(InvoiceStatus status);

    @Query("select invoice.invoiceId from Invoice invoice where invoice.invoiceType = :type "
            + "and invoice.status = :status and invoice.contract.status = :contractStatus "
            + "and invoice.dueDate < :cutoff "
            + "order by invoice.invoiceId")
    List<Long> findOverdueDepositIds(@Param("type") InvoiceType type,
            @Param("status") InvoiceStatus status,
            @Param("contractStatus") com.dormhub.entity.enums.ContractStatus contractStatus,
            @Param("cutoff") LocalDateTime cutoff);
}
