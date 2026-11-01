package com.dormhub.repository;

import com.dormhub.entity.Invoice;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.Invoice;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByRoom_IdAndBillingMonth(Long roomId, YearMonth billingMonth);

    boolean existsByContract_ContractIdAndBillingMonth(Long contractId, YearMonth billingMonth);

    java.util.Optional<Invoice> findByContract_ContractIdAndInvoiceType(Long contractId, InvoiceType invoiceType);

    List<Invoice> findAllByContract_ContractIdInAndInvoiceType(
            Collection<Long> contractIds, InvoiceType invoiceType);

    Page<Invoice> findByBillingMonth(YearMonth billingMonth, Pageable pageable);
}
