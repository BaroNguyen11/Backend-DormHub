package com.dormhub.service;

import com.dormhub.dto.response.DepositFlowResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.InvoiceDetail;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.InvoiceItemType;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.InvoiceCreationLockRepository;
import com.dormhub.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceCreationLockRepository invoiceCreationLockRepository;
    private final RoomAssignmentService roomAssignmentService;

    public DepositService(InvoiceRepository invoiceRepository,
                          InvoiceCreationLockRepository invoiceCreationLockRepository,
                          RoomAssignmentService roomAssignmentService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceCreationLockRepository = invoiceCreationLockRepository;
        this.roomAssignmentService = roomAssignmentService;
    }

    @Transactional
    public DepositFlowResponse createDepositInvoice(Long contractId) {
        if (contractId == null || contractId <= 0) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID, "contractId phải là số dương");
        }

        Contract contract = invoiceCreationLockRepository.lockContract(contractId);
        if (contract == null) {
            throw new AppException(ErrorCode.BILLING_NOT_FOUND, "Không tìm thấy hợp đồng");
        }
        if (!isWaitingForDeposit(contract)) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Chỉ có thể tạo hóa đơn đặt cọc cho hợp đồng DRAFT hoặc PENDING");
        }
        if (contract.getDepositAmount() == null || contract.getDepositAmount().signum() <= 0) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID,
                    "Số tiền đặt cọc của hợp đồng phải lớn hơn 0");
        }
        if (contract.getDepositStatus() != null
                && contract.getDepositStatus() != DepositStatus.UNPAID) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Hợp đồng không còn ở trạng thái chờ thanh toán cọc");
        }
        if (invoiceRepository.existsByContract_ContractIdAndInvoiceType(
                contractId, InvoiceType.DEPOSIT)) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Hợp đồng đã có hóa đơn đặt cọc");
        }

        contract.setDepositStatus(DepositStatus.UNPAID);
        Invoice invoice = new Invoice();
        invoice.setInvoiceType(InvoiceType.DEPOSIT);
        invoice.setContract(contract);
        LocalDateTime issueDate = LocalDateTime.now(BUSINESS_ZONE);
        invoice.setBillingMonth(YearMonth.from(issueDate));
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(contract.getStartDate());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setTotalAmount(contract.getDepositAmount());

        InvoiceDetail detail = new InvoiceDetail();
        detail.setInvoice(invoice);
        detail.setItemType(InvoiceItemType.DEPOSIT);
        detail.setDescription("Tiền đặt cọc hợp đồng " + contractId);
        detail.setQuantity(BigDecimal.ONE);
        detail.setUnitPrice(contract.getDepositAmount());
        detail.setAmount(contract.getDepositAmount());
        invoice.getDetails().add(detail);

        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);
        return toResponse(contract, savedInvoice);
    }

    // PaymentService calls this after recording a successful deposit payment.
    @Transactional
    public DepositFlowResponse confirmDepositPaid(Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID, "invoiceId phải là số dương");
        }

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND,
                        "Không tìm thấy hóa đơn đặt cọc"));
        if (invoice.getInvoiceType() != InvoiceType.DEPOSIT || invoice.getContract() == null) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID,
                    "Hóa đơn không thuộc luồng đặt cọc hợp đồng");
        }

        Contract contract = invoiceCreationLockRepository
                .lockContract(invoice.getContract().getContractId());
        if (contract == null) {
            throw new AppException(ErrorCode.BILLING_NOT_FOUND, "Không tìm thấy hợp đồng");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID
                && contract.getStatus() == ContractStatus.ACTIVE
                && contract.getDepositStatus() == DepositStatus.HELD) {
            return toResponse(contract, invoice);
        }
        if (invoice.getStatus() != InvoiceStatus.UNPAID
                && invoice.getStatus() != InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Hóa đơn không ở trạng thái có thể xác nhận thanh toán");
        }
        if (!isWaitingForDeposit(contract)) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Chỉ có thể kích hoạt hợp đồng đang chờ thanh toán cọc");
        }
        if (contract.getDepositAmount() == null || invoice.getTotalAmount() == null
                || invoice.getTotalAmount().compareTo(contract.getDepositAmount()) != 0) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Số tiền hóa đơn đặt cọc không khớp với hợp đồng");
        }
        if (contract.getDepositStatus() != null
                && contract.getDepositStatus() != DepositStatus.UNPAID) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Trạng thái đặt cọc của hợp đồng không hợp lệ");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setDepositStatus(DepositStatus.HELD);
        roomAssignmentService.assign(contract.getContractId());
        return toResponse(contract, invoice);
    }

    private boolean isWaitingForDeposit(Contract contract) {
        return contract.getStatus() == ContractStatus.DRAFT
                || contract.getStatus() == ContractStatus.PENDING;
    }

    private DepositFlowResponse toResponse(Contract contract, Invoice invoice) {
        return new DepositFlowResponse(
                contract.getContractId(),
                invoice.getInvoiceId(),
                invoice.getStatus(),
                contract.getStatus(),
                contract.getDepositStatus(),
                invoice.getTotalAmount(),
                invoice.getDueDate());
    }
}
