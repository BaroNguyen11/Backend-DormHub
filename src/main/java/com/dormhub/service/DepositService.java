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
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;

    public DepositService(InvoiceRepository invoiceRepository, ContractRepository contractRepository) {
        this.invoiceRepository = invoiceRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional
    public DepositFlowResponse createDepositInvoice(Long contractId) {
        return createDepositInvoice(contractId, false);
    }

    @Transactional
    public DepositFlowResponse createDepositInvoiceIfAbsent(Long contractId) {
        return createDepositInvoice(contractId, true);
    }

    private DepositFlowResponse createDepositInvoice(Long contractId, boolean returnExisting) {
        if (contractId == null || contractId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ID hợp đồng không hợp lệ. ID phải là số nguyên dương.");
        }

        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND, "Không tìm thấy hợp đồng"));
        if (returnExisting) {
            var existing = invoiceRepository.findByContract_ContractIdAndInvoiceType(contractId, InvoiceType.DEPOSIT);
            if (existing.isPresent()) {
                return toResponse(contract, existing.get());
            }
        }
        if (!isWaitingForDeposit(contract)) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Chỉ có thể tạo hóa đơn đặt cọc cho hợp đồng PENDING");
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

        BigDecimal amount = contract.getDepositAmount().setScale(0, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID, "Số tiền đặt cọc sau làm tròn phải lớn hơn 0");
        }
        contract.setDepositStatus(DepositStatus.UNPAID);
        Invoice invoice = new Invoice();
        invoice.setInvoiceType(InvoiceType.DEPOSIT);
        invoice.setContract(contract);
        LocalDateTime issueDate = LocalDateTime.now(BUSINESS_ZONE);
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(issueDate.plusHours(48));
        invoice.setStatus(InvoiceStatus.UNPAID);

        InvoiceDetail detail = new InvoiceDetail();
        detail.setInvoice(invoice);
        detail.setItemType(InvoiceItemType.DEPOSIT);
        detail.setDescription("Tiền đặt cọc hợp đồng " + contractId);
        detail.setQuantity(BigDecimal.ONE);
        detail.setUnitPrice(amount);
        detail.setAmount(detail.getQuantity().multiply(detail.getUnitPrice()).setScale(0, RoundingMode.HALF_UP));
        invoice.getDetails().add(detail);
        invoice.setTotalAmount(detail.getAmount());

        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);
        return toResponse(contract, savedInvoice);
    }

    // PaymentService calls this after recording a successful deposit payment.
    @Transactional
    public DepositFlowResponse confirmDepositPaid(Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ID hóa đơn không hợp lệ. ID phải là số nguyên dương.");
        }

        Invoice candidate = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND,
                        "Không tìm thấy hóa đơn đặt cọc"));
        if (candidate.getInvoiceType() != InvoiceType.DEPOSIT || candidate.getContract() == null) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID,
                    "Hóa đơn không thuộc luồng đặt cọc hợp đồng");
        }

        Long contractId = candidate.getContract().getContractId();
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND, "Không tìm thấy hợp đồng"));
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND,
                        "Không tìm thấy hóa đơn đặt cọc"));
        if (invoice.getInvoiceType() != InvoiceType.DEPOSIT || invoice.getContract() == null
                || !contractId.equals(invoice.getContract().getContractId())) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID,
                    "Hóa đơn không thuộc luồng đặt cọc hợp đồng");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID
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
                    "Chỉ có thể xác nhận tiền cọc cho hợp đồng PENDING");
        }
        if (contract.getDepositAmount() == null || invoice.getTotalAmount() == null
                || invoice.getTotalAmount().compareTo(
                        contract.getDepositAmount().setScale(0, RoundingMode.HALF_UP)) != 0) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Số tiền hóa đơn đặt cọc không khớp với hợp đồng");
        }
        if (contract.getDepositStatus() != null
                && contract.getDepositStatus() != DepositStatus.UNPAID) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Trạng thái đặt cọc của hợp đồng không hợp lệ");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        contract.setDepositStatus(DepositStatus.HELD);
        return toResponse(contract, invoice);
    }

    private boolean isWaitingForDeposit(Contract contract) {
        return contract.getStatus() == ContractStatus.PENDING;
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
