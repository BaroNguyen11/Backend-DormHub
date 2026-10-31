package com.dormhub.mapper;

import com.dormhub.dto.response.InvoiceDetailResponse;
import com.dormhub.dto.response.InvoiceResponse;
import com.dormhub.dto.response.InvoiceSummaryResponse;
import com.dormhub.entity.Invoice;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public InvoiceSummaryResponse toSummary(Invoice invoice, boolean overdue) {
        return new InvoiceSummaryResponse(
                invoice.getInvoiceId(), invoice.getInvoiceType(), roomId(invoice), contractId(invoice),
                invoice.getBillingMonth(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getStatus(), overdue, invoice.getTotalAmount());
    }

    public InvoiceResponse toResponse(Invoice invoice, boolean overdue) {
        List<InvoiceDetailResponse> details = invoice.getDetails().stream()
                .map(detail -> new InvoiceDetailResponse(
                        detail.getDetailId(), detail.getItemType(), detail.getDescription(),
                        detail.getQuantity(), detail.getUnitPrice(), detail.getAmount()))
                .toList();
        return new InvoiceResponse(
                invoice.getInvoiceId(), invoice.getInvoiceType(), roomId(invoice), contractId(invoice),
                invoice.getBillingMonth(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getStatus(), overdue, invoice.getTotalAmount(), details);
    }

    private Long roomId(Invoice invoice) {
        return invoice.getRoom() != null ? invoice.getRoom().getId()
                : invoice.getContract().getRoom().getId();
    }

    private Long contractId(Invoice invoice) {
        return invoice.getContract() == null ? null : invoice.getContract().getContractId();
    }
}
