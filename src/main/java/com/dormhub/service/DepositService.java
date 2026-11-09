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
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.InvoiceCreationLockRepository;
import com.dormhub.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceCreationLockRepository invoiceCreationLockRepository;
    private final NotificationService notificationService;
    private final RoomAssignmentService roomAssignmentService;
    private final InvoiceService invoiceService;

    public DepositService(InvoiceRepository invoiceRepository,
                          InvoiceCreationLockRepository invoiceCreationLockRepository,
                          NotificationService notificationService,
                          RoomAssignmentService roomAssignmentService,
                          InvoiceService invoiceService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceCreationLockRepository = invoiceCreationLockRepository;
        this.notificationService = notificationService;
        this.roomAssignmentService = roomAssignmentService;
        this.invoiceService = invoiceService;
    }

    @Transactional
    public DepositFlowResponse createDepositInvoice(Long contractId) {
        if (contractId == null || contractId <= 0) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID, "contractId phải là số dương");
        }
        Contract contract = invoiceCreationLockRepository.lockContract(contractId);
        if (contract == null) {
            throw new AppException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        if (contract.getStatus() != ContractStatus.PENDING
                || contract.getDepositStatus() != DepositStatus.UNPAID) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Chỉ tạo hóa đơn cọc cho hợp đồng PENDING chưa thanh toán");
        }
        if (contract.getDepositAmount() == null || contract.getDepositAmount().signum() <= 0) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID, "Tiền cọc phải lớn hơn 0");
        }
        if (invoiceRepository.existsByContract_ContractIdAndInvoiceType(contractId, InvoiceType.DEPOSIT)) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT, "Hợp đồng đã có hóa đơn cọc");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceType(InvoiceType.DEPOSIT);
        invoice.setContract(contract);
        invoice.setIssueDate(LocalDateTime.now(BUSINESS_ZONE));
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

        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        return toResponse(contract, saved);
    }

    // Called by the payment flow only after a successful payment has been recorded.
    @Transactional
    public DepositFlowResponse confirmDepositPaid(Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID, "invoiceId phải là số dương");
        }
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND));
        if (invoice.getInvoiceType() != InvoiceType.DEPOSIT || invoice.getContract() == null) {
            throw new AppException(ErrorCode.DEPOSIT_INVALID, "Hóa đơn không thuộc luồng đặt cọc");
        }
        Contract contract = invoiceCreationLockRepository.lockContract(
                invoice.getContract().getContractId());
        if (contract == null) {
            throw new AppException(ErrorCode.CONTRACT_NOT_FOUND);
        }

        if (invoice.getStatus() == InvoiceStatus.PAID
                && contract.getStatus() == ContractStatus.ACTIVE
                && contract.getDepositStatus() == DepositStatus.HELD) {
            return toResponse(contract, invoice);
        }
        if (invoice.getStatus() != InvoiceStatus.UNPAID
                || contract.getStatus() != ContractStatus.PENDING
                || contract.getDepositStatus() != DepositStatus.UNPAID
                || contract.getDepositAmount() == null
                || invoice.getTotalAmount() == null
                || invoice.getTotalAmount().compareTo(contract.getDepositAmount()) != 0) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Hóa đơn hoặc hợp đồng không còn ở trạng thái chờ thanh toán cọc");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setDepositStatus(DepositStatus.HELD);
        roomAssignmentService.assign(contract.getContractId());

        // P0: sinh ngay hóa đơn tiền phòng tháng đầu theo tỷ lệ ngày ở (proration)
        invoiceService.createFirstMonthRoomFeeIfAbsent(contract);

        if (contract.getStudent() != null && contract.getStudent().getUser() != null) {
            notificationService.sendNotification(
                    NotificationType.CONTRACT_ACTIVATED,
                    "Hợp đồng đã được kích hoạt",
                    String.format("Thanh toán tiền cọc thành công. Hợp đồng #%d đã ACTIVE và bạn đã được phân phòng.",
                            contract.getContractId()),
                    invoice.getInvoiceId(),
                    contract.getStudent().getUser().getId());
        }
        return toResponse(contract, invoice);
    }

    private DepositFlowResponse toResponse(Contract contract, Invoice invoice) {
        return new DepositFlowResponse(
                contract.getContractId(), invoice.getInvoiceId(), invoice.getStatus(),
                contract.getStatus(), contract.getDepositStatus(), invoice.getTotalAmount(),
                invoice.getDueDate());
    }
}
