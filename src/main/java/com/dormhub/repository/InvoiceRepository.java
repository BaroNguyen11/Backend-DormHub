package com.dormhub.repository;

import com.dormhub.entity.Invoice;
import com.dormhub.entity.enums.InvoiceType;
import java.time.YearMonth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByRoom_IdAndBillingMonth(Long roomId, YearMonth billingMonth);

    boolean existsByContract_ContractIdAndBillingMonth(Long contractId, YearMonth billingMonth);

    boolean existsByContract_ContractIdAndInvoiceType(Long contractId, InvoiceType invoiceType);

    Page<Invoice> findByBillingMonth(YearMonth billingMonth, Pageable pageable);
}
