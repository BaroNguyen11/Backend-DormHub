package com.dormhub.service;

import com.dormhub.dto.request.InvoiceCreateRequest;
import com.dormhub.dto.response.InvoicePageResponse;
import com.dormhub.dto.response.InvoiceResponse;
import com.dormhub.dto.response.InvoiceSummaryResponse;
import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.InvoiceDetail;
import com.dormhub.entity.enums.InvoiceItemType;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.Contract;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.enums.ReadingStatus;
import com.dormhub.entity.Room;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.InvoiceMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceCreationLockRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.MeterReadingRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class InvoiceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceCreationLockRepository invoiceCreationLockRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final RoomMonthlyChargeCalculator calculator;
    private final InvoiceMapper invoiceMapper;
    private final NotificationService notificationService;
    private final ContractRepository contractRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceCreationLockRepository invoiceCreationLockRepository,
            MeterReadingRepository meterReadingRepository, RoomMonthlyChargeCalculator calculator,
            InvoiceMapper invoiceMapper, NotificationService notificationService, ContractRepository contractRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceCreationLockRepository = invoiceCreationLockRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.calculator = calculator;
        this.invoiceMapper = invoiceMapper;
        this.notificationService = notificationService;
        this.contractRepository = contractRepository;
    }

    @Transactional
    public InvoiceResponse create(InvoiceCreateRequest request) {
        if (request == null || request.getInvoiceType() == null || request.getInvoiceType().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "invoiceType is required");
        }
        InvoiceType type = parseType(request.getInvoiceType());
        if (type == InvoiceType.DEPOSIT) {
            throw new AppException(ErrorCode.DEPOSIT_NOT_SUPPORTED,
                    "DEPOSIT invoice creation is not supported in this phase");
        }
        YearMonth month = parseMonth(request.getBillingMonth());
        if (type == InvoiceType.ROOM_FEE) {
            return createRoomFee(request, month);
        }
        if (request.getContractId() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "contractId is only valid for ROOM_FEE");
        }
        if (request.getRoomId() == null || request.getRoomId() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "A positive roomId is required");
        }
        Room room = invoiceCreationLockRepository.lockRoom(request.getRoomId());
        if (room == null) {
            throw new AppException(ErrorCode.BILLING_NOT_FOUND, "Room does not exist");
        }
        MeterReading reading = meterReadingRepository
                .findByRoom_IdAndReadingMonth(request.getRoomId(), month);
        if (reading == null) {
            throw new AppException(ErrorCode.BILLING_NOT_FOUND, "Meter reading does not exist");
        }
        if (reading.getStatus() != ReadingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Meter reading is not confirmed");
        }
        if (invoiceRepository.existsByRoom_IdAndBillingMonth(request.getRoomId(), month)) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "UTILITY invoice already exists for this room and month");
        }

        RoomMonthlyChargeResult charge = calculator.calculate(request.getRoomId(), month, reading);

        InvoiceDetail electricity = newDetail(
                InvoiceItemType.ELECTRICITY, "Electricity", charge.getElectricityConsumption(),
                reading.getElectricityPrice(), charge.getElectricityAmount());
        InvoiceDetail water = newDetail(
                InvoiceItemType.WATER, "Water", charge.getWaterConsumption(),
                reading.getWaterPrice(), charge.getWaterAmount());
        BigDecimal total = electricity.getAmount().add(water.getAmount());

        Invoice invoice = newMonthlyInvoice(InvoiceType.UTILITY, month, total);
        invoice.setRoom(room);

        // Query active contract for this room in the billing month
        String billingMonthStr = month.toString(); // "2026-11"
        Optional<Contract> activeContract = contractRepository
            .findActiveContractByRoomAndMonth(room.getId(), billingMonthStr);

        if (activeContract.isPresent()) {
            invoice.setContract(activeContract.get());
        } else {
            // Log warning but allow invoice creation without contract
            // (Room might have previous unpaid invoices even if no current contract)
            log.warn("No active contract found for room {} in month {}, creating invoice without contract link", 
                     room.getId(), billingMonthStr);
        }

        attachDetail(invoice, electricity);
        attachDetail(invoice, water);
        try {
            Invoice saved = invoiceRepository.saveAndFlush(invoice);
            
            // Send notification after invoice creation
            if (saved.getContract() != null 
                && saved.getContract().getStudent() != null 
                && saved.getContract().getStudent().getUser() != null) {
                String dueDate = saved.getDueDate() != null 
                    ? saved.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "N/A";
                notificationService.sendNotification(
                    NotificationType.INVOICE_CREATED,
                    "Hóa đơn mới",
                    String.format("Hóa đơn %s đã được tạo. Hạn thanh toán: %s", 
                        saved.getInvoiceType().name(), dueDate),
                    saved.getInvoiceId(),
                    saved.getContract().getStudent().getUser().getId()
                );
            }
            
            return invoiceMapper.toResponse(saved, overdue(saved));
        } catch (DataIntegrityViolationException exception) {
            throw mapIntegrityError(exception);
        }
    }

    private InvoiceResponse createRoomFee(InvoiceCreateRequest request, YearMonth month) {
        if (request.getContractId() == null || request.getContractId() <= 0 || request.getRoomId() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ROOM_FEE requires a positive contractId and no roomId");
        }
        Contract contract = invoiceCreationLockRepository.lockContract(request.getContractId());
        if (contract == null) {
            throw new AppException(ErrorCode.BILLING_NOT_FOUND, "Contract does not exist");
        }
        validateRoomFeeContract(contract, month);
        if (invoiceRepository.existsByContract_ContractIdAndBillingMonth(request.getContractId(), month)) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "ROOM_FEE invoice already exists for this contract and month");
        }
        return saveRoomFeeInvoice(contract, month);
    }

    @Transactional
    public boolean createRoomFeeIfAbsent(Long contractId, YearMonth month) {
        if (contractId == null || contractId <= 0 || month == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "A positive contractId and billing month are required");
        }
        Contract contract = invoiceCreationLockRepository.lockContract(contractId);
        if (contract == null) {
            throw new AppException(ErrorCode.BILLING_NOT_FOUND, "Contract does not exist");
        }
        if (invoiceRepository.existsByContract_ContractIdAndBillingMonth(contractId, month)) {
            return false;
        }
        if (YearMonth.from(contract.getStartDate()).equals(month)) {
            throw new AppException(ErrorCode.BILLING_CONFLICT,
                    "First-month ROOM_FEE must be created by Contract/Billing integration");
        }
        validateRoomFeeContract(contract, month);
        saveRoomFeeInvoice(contract, month);
        return true;
    }

    private void validateRoomFeeContract(Contract contract, YearMonth month) {
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        if (contract.getStatus() != ContractStatus.ACTIVE
                || contract.getStartDate().toLocalDate().isAfter(last)
                || contract.getEndDate().toLocalDate().isBefore(first)) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Contract is not active in the billing month");
        }
        if (contract.getMonthlyRent() == null || contract.getMonthlyRent().signum() < 0) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Contract monthly rent is invalid");
        }
    }

    private InvoiceResponse saveRoomFeeInvoice(Contract contract, YearMonth month) {
        BigDecimal rent = contract.getMonthlyRent().setScale(0, java.math.RoundingMode.HALF_UP);
        Invoice invoice = newMonthlyInvoice(InvoiceType.ROOM_FEE, month, rent);
        invoice.setContract(contract);
        attachDetail(invoice, newDetail(InvoiceItemType.ROOM, "Room rent", BigDecimal.ONE, rent, rent));
        try {
            Invoice saved = invoiceRepository.saveAndFlush(invoice);
            
            // Send notification after ROOM_FEE invoice creation
            if (saved.getContract() != null 
                && saved.getContract().getStudent() != null 
                && saved.getContract().getStudent().getUser() != null) {
                String dueDate = saved.getDueDate() != null 
                    ? saved.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "N/A";
                notificationService.sendNotification(
                    NotificationType.INVOICE_CREATED,
                    "Hóa đơn mới",
                    String.format("Hóa đơn %s đã được tạo. Hạn thanh toán: %s", 
                        saved.getInvoiceType().name(), dueDate),
                    saved.getInvoiceId(),
                    saved.getContract().getStudent().getUser().getId()
                );
            }
            
            return invoiceMapper.toResponse(saved, overdue(saved));
        } catch (DataIntegrityViolationException exception) {
            throw mapIntegrityError(exception);
        }
    }

    private RuntimeException mapIntegrityError(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return new AppException(ErrorCode.BILLING_CONFLICT,
                        "Invoice violates a database constraint or already exists");
            }
            cause = cause.getCause();
        }
        return exception;
    }

    @Transactional(readOnly = true)
    public InvoicePageResponse list(String month, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "page must be nonnegative and size must be between 1 and 100");
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invoiceId"));
        Page<Invoice> invoices = month == null
                ? invoiceRepository.findAll(pageable)
                : invoiceRepository.findByBillingMonth(parseMonth(month), pageable);
        List<InvoiceSummaryResponse> content = invoices.getContent().stream()
                .map(invoice -> invoiceMapper.toSummary(invoice, overdue(invoice)))
                .toList();
        return new InvoicePageResponse(content, page, size, invoices.getTotalElements(), invoices.getTotalPages());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "A positive invoiceId is required");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND, "Invoice does not exist"));
        return invoiceMapper.toResponse(invoice, overdue(invoice));
    }

    @Transactional(readOnly = true)
    public InvoicePageResponse listByStudent(Long userId, InvoiceStatus status, int page, int size) {
        if (userId == null || userId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "A positive userId is required");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "page must be nonnegative and size must be between 1 and 100");
        }
        
        // Find all contracts belonging to this student
        List<Long> contractIds = contractRepository.findAll().stream()
                .filter(contract -> contract.getStudent() != null 
                        && contract.getStudent().getUser() != null
                        && contract.getStudent().getUser().getId().equals(userId))
                .map(Contract::getContractId)
                .toList();
        
        if (contractIds.isEmpty()) {
            // Student has no contracts, return empty page
            return new InvoicePageResponse(List.of(), page, size, 0L, 0);
        }
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invoiceId"));
        Page<Invoice> invoices = status == null
                ? invoiceRepository.findByContract_ContractIdIn(contractIds, pageable)
                : invoiceRepository.findByContract_ContractIdInAndStatus(contractIds, status, pageable);
        
        List<InvoiceSummaryResponse> content = invoices.getContent().stream()
                .map(invoice -> invoiceMapper.toSummary(invoice, overdue(invoice)))
                .toList();
        
        return new InvoicePageResponse(content, page, size, invoices.getTotalElements(), invoices.getTotalPages());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getByStudent(Long invoiceId, Long userId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "A positive invoiceId is required");
        }
        if (userId == null || userId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "A positive userId is required");
        }
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND, "Invoice does not exist"));
        
        // Verify ownership: invoice must belong to a contract of this student
        if (invoice.getContract() == null
                || invoice.getContract().getStudent() == null
                || invoice.getContract().getStudent().getUser() == null
                || !invoice.getContract().getStudent().getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "You do not have permission to view this invoice");
        }
        
        return invoiceMapper.toResponse(invoice, overdue(invoice));
    }

    private InvoiceType parseType(String value) {
        try {
            return InvoiceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "invoiceType must be UTILITY, ROOM_FEE or DEPOSIT");
        }
    }

    private Invoice newMonthlyInvoice(InvoiceType type, YearMonth month, BigDecimal totalAmount) {
        LocalDateTime issueDate = LocalDateTime.now(BUSINESS_ZONE);
        Invoice invoice = new Invoice();
        invoice.setInvoiceType(type);
        invoice.setBillingMonth(month);
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(issueDate.plusDays(14));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setTotalAmount(totalAmount);
        return invoice;
    }

    private void attachDetail(Invoice invoice, InvoiceDetail detail) {
        detail.setInvoice(invoice);
        invoice.getDetails().add(detail);
    }

    private InvoiceDetail newDetail(InvoiceItemType itemType, String description,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        InvoiceDetail detail = new InvoiceDetail();
        detail.setItemType(itemType);
        detail.setDescription(description);
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        detail.setAmount(amount);
        return detail;
    }

    private YearMonth parseMonth(String value) {
        if (value == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "billingMonth is required");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "billingMonth must use YYYY-MM");
        }
    }

    private boolean overdue(Invoice invoice) {
        return invoice.getDueDate() != null && invoice.getStatus() == InvoiceStatus.UNPAID
                && invoice.getDueDate().toLocalDate().isBefore(LocalDate.now(BUSINESS_ZONE));
    }
}
