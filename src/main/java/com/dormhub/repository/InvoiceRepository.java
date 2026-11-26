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
    // UTILITY belongs to the room's actual residents in the billed month, including former residents.
    String STUDENT_ACCESS = """
        ((i.invoice_type <> 'UTILITY' and exists (
          select 1 from contracts c join students s on s.id=c.student_id
          where c.contract_id=i.contract_id and s.user_id=:userId))
        or (i.invoice_type='UTILITY' and exists (
          select 1 from room_assignments a join students s on s.id=a.student_id
          where a.room_id=i.room_id and s.user_id=:userId
          and a.check_in_at is not null
          and a.check_in_at < i.billing_month + interval '1 month'
          and (a.check_out_at is null or a.check_out_at > i.billing_month))))
        """;

    String SEARCH_FROM = " from invoices i left join contracts c on c.contract_id=i.contract_id "
            + "left join students s on s.id=c.student_id left join users u on u.id=s.user_id "
            + "left join dorm_applications da on da.id=c.dorm_application_id "
            + "left join lateral (select a.room_id from room_assignments a where a.contract_id=c.contract_id "
            + "and (i.invoice_type='DEPOSIT' or (coalesce(a.check_in_at,a.assigned_at)<i.billing_month+interval '1 month' "
            + "and (a.check_out_at is null or a.check_out_at>i.billing_month))) order by a.assignment_id limit 1) history on true "
            + "left join rooms r on r.id=coalesce(i.room_id,case when i.invoice_type='DEPOSIT' then da.requested_room_id end,"
            + "history.room_id,case when c.status='PENDING' and i.invoice_type='DEPOSIT' then c.room_id end) "
            + "left join buildings b on b.id=r.building_id ";
    // Identical ownership/period rule to invoice viewing and payment, including former rooms.
    @Query(value = "select exists(select 1 from invoices i where i.status='UNPAID' "
            + "and i.invoice_type in ('ROOM_FEE','UTILITY') and " + STUDENT_ACCESS + ")", nativeQuery = true)
    boolean hasUnpaidResidenceObligation(@Param("userId") Long userId);
    String SEARCH_TEXT = "translate(lower(concat_ws(' ',i.invoice_id,i.invoice_type,i.billing_month,i.total_amount,"
            + "r.room_number,b.name,s.student_code,s.full_name,u.full_name,"
            + "case i.invoice_type when 'UTILITY' then 'Hóa đơn chung của phòng điện nước' when 'ROOM_FEE' then 'Tiền thuê phòng' else 'Tiền cọc' end)), '"
            + com.dormhub.specification.SearchSpecifications.ACCENTS + "', '"
            + com.dormhub.specification.SearchSpecifications.ASCII + "')";
    String SEARCH_FILTER = " and (cast(:month as date) is null or i.billing_month=cast(:month as date)) "
            + "and (:status='' or i.status=:status) and (:search='' or not exists "
            + "(select 1 from unnest(string_to_array(:search,' ')) as terms(term) where "
            + SEARCH_TEXT + " not like '%' || replace(replace(replace(term,'!','!!'),'%','!%'),'_','!_') || '%' escape '!')) ";

    @Query(value = "select i.*" + SEARCH_FROM + " where " + STUDENT_ACCESS
            + SEARCH_FILTER + " order by i.invoice_id desc",
            countQuery = "select count(*)" + SEARCH_FROM + " where " + STUDENT_ACCESS + SEARCH_FILTER, nativeQuery = true)
    Page<Invoice> findAccessibleInvoices(@Param("userId") Long userId,
            @Param("month") java.time.LocalDate month, @Param("search") String search,
            @Param("status") String status, Pageable pageable);

    @Query(value = "select i.*" + SEARCH_FROM + " where true " + SEARCH_FILTER + " order by i.invoice_id desc",
            countQuery = "select count(*)" + SEARCH_FROM + " where true " + SEARCH_FILTER, nativeQuery = true)
    Page<Invoice> findManagerInvoices(@Param("month") java.time.LocalDate month,
            @Param("search") String search, @Param("status") String status, Pageable pageable);

    @Query(value = "select i.* from invoices i where i.invoice_id=:invoiceId and " + STUDENT_ACCESS,
            nativeQuery = true)
    Optional<Invoice> findAccessibleInvoice(@Param("invoiceId") Long invoiceId, @Param("userId") Long userId);

    boolean existsByRoom_IdAndBillingMonthAndInvoiceType(Long roomId, YearMonth month, InvoiceType type);

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
