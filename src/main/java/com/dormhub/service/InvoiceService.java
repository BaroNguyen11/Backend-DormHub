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
import com.dormhub.repository.RoomAssignmentRepository;
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
    private final RoomAssignmentRepository roomAssignmentRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceCreationLockRepository invoiceCreationLockRepository,
            MeterReadingRepository meterReadingRepository, RoomMonthlyChargeCalculator calculator,
            InvoiceMapper invoiceMapper, NotificationService notificationService, RoomAssignmentRepository roomAssignmentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceCreationLockRepository = invoiceCreationLockRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.calculator = calculator;
        this.invoiceMapper = invoiceMapper;
        this.notificationService = notificationService;
        this.roomAssignmentRepository = roomAssignmentRepository;
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
        return createUtilityInvoice(room, month);
    }

    @Transactional
    public boolean createUtilityIfAbsent(Long roomId, YearMonth month) {
        if (roomId == null || roomId <= 0 || month == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Room room = invoiceCreationLockRepository.lockRoom(roomId);
        if (room == null) throw new AppException(ErrorCode.BILLING_NOT_FOUND);
        if (invoiceRepository.existsByRoom_IdAndBillingMonthAndInvoiceType(roomId, month, InvoiceType.UTILITY)) {
            return false;
        }
        createUtilityInvoice(room, month);
        return true;
    }

    private InvoiceResponse createUtilityInvoice(Room room, YearMonth month) {
        if (!month.isBefore(YearMonth.now(BUSINESS_ZONE))) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Kỳ điện nước chưa kết thúc");
        }
        MeterReading reading = meterReadingRepository
                .findByRoom_IdAndReadingMonth(room.getId(), month);
        if (reading == null) {
            throw new AppException(ErrorCode.BILLING_NOT_FOUND, "Meter reading does not exist");
        }
        if (reading.getStatus() != ReadingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Meter reading is not confirmed");
        }
        if (invoiceRepository.existsByRoom_IdAndBillingMonth(room.getId(), month)) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "UTILITY invoice already exists for this room and month");
        }

        RoomMonthlyChargeResult charge = calculator.calculate(room.getId(), month, reading);

        InvoiceDetail electricity = newDetail(
                InvoiceItemType.ELECTRICITY, "Electricity", charge.getElectricityConsumption(),
                reading.getElectricityPrice(), charge.getElectricityAmount());
        InvoiceDetail water = newDetail(
                InvoiceItemType.WATER, "Water", charge.getWaterConsumption(),
                reading.getWaterPrice(), charge.getWaterAmount());
        BigDecimal total = electricity.getAmount().add(water.getAmount());

        Invoice invoice = newMonthlyInvoice(InvoiceType.UTILITY, month, total);
        invoice.setRoom(room);

        // Shared room invoice: the nullable contract association is intentionally unused.
        // Ownership and recipients come from actual occupancy, not a representative contract.
        if (utilityRecipients(invoice).isEmpty()) {
            throw new AppException(ErrorCode.BILLING_CONFLICT,
                    "Không có lịch sử cư trú trong kỳ — không phát hành hóa đơn điện nước");
        }

        attachDetail(invoice, electricity);
        attachDetail(invoice, water);
        try {
            Invoice saved = invoiceRepository.saveAndFlush(invoice);
            notifyInvoiceCreated(saved);
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
        validateRoomFeeContract(contract, month);
        saveRoomFeeInvoice(contract, month);
        return true;
    }

    /**
     * P0 - Tiền phòng tháng đầu theo tỷ lệ ngày ở (proration):
     * thành tiền = monthlyRent × effectiveDays / daysInMonth (HALF_UP, 0 chữ số).
     * Được gọi khi hợp đồng kích hoạt (cọc hợp lệ) để không mất doanh thu kỳ đầu.
     * Chỉ phát hành khi đã đến ngày bắt đầu thuê; bỏ qua nếu kỳ đầu đã có hóa đơn.
     */
    @Transactional
    public void createFirstMonthRoomFeeIfAbsent(Contract contract) {
        if (contract == null || contract.getContractId() == null
                || contract.getStartDate() == null || contract.getMonthlyRent() == null
                || contract.getStartDate().toLocalDate().isAfter(LocalDate.now(BUSINESS_ZONE))) {
            return;
        }
        createRoomFeeIfAbsent(contract.getContractId(), YearMonth.from(contract.getStartDate()));
    }

    public static BigDecimal calculateFirstMonthRent(BigDecimal monthlyRent, LocalDate startDate) {
        int daysInMonth = startDate.lengthOfMonth();
        int effectiveDays = daysInMonth - startDate.getDayOfMonth() + 1;
        return monthlyRent.multiply(BigDecimal.valueOf(effectiveDays))
                .divide(BigDecimal.valueOf(daysInMonth), 0, java.math.RoundingMode.HALF_UP);
    }

    private void notifyInvoiceCreated(Invoice invoice) {
        if (invoice.getInvoiceType() == InvoiceType.UTILITY) {
            List<Long> recipients = utilityRecipients(invoice);
            notificationService.sendNotification(NotificationType.INVOICE_CREATED,
                    "Hóa đơn điện nước của phòng",
                    String.format("Hóa đơn điện nước chung phòng %s, kỳ %s đã được phát hành. Hạn thanh toán: %s. Một người thanh toán đủ sẽ hoàn tất hóa đơn của phòng.",
                            invoice.getRoom().getRoomNumber(), invoice.getBillingMonth(),
                            invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                    invoice.getInvoiceId(), recipients);
            return;
        }
        if (invoice.getContract() != null
                && invoice.getContract().getStudent() != null
                && invoice.getContract().getStudent().getUser() != null) {
            String dueDate = invoice.getDueDate() != null
                    ? invoice.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "N/A";
            notificationService.sendNotification(
                    NotificationType.INVOICE_CREATED,
                    "Hóa đơn mới",
                    String.format("Hóa đơn %s đã được tạo. Hạn thanh toán: %s",
                            invoice.getInvoiceType().name(), dueDate),
                    invoice.getInvoiceId(),
                    invoice.getContract().getStudent().getUser().getId()
            );
        }
    }

    private List<Long> utilityRecipients(Invoice invoice) {
        YearMonth month = invoice.getBillingMonth();
        return roomAssignmentRepository.findResidentUserIdsForPeriod(invoice.getRoom().getId(),
                month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
    }

    private void validateRoomFeeContract(Contract contract, YearMonth month) {
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        if (contract.getStatus() != ContractStatus.ACTIVE
                || contract.getStartDate() == null || contract.getEndDate() == null
                || contract.getStartDate().toLocalDate().isAfter(LocalDate.now(BUSINESS_ZONE))
                || contract.getStartDate().toLocalDate().isAfter(last)
                || contract.getEndDate().toLocalDate().isBefore(first)) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Contract is not active in the billing month");
        }
        if (contract.getMonthlyRent() == null || contract.getMonthlyRent().signum() < 0) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Contract monthly rent is invalid");
        }
    }

    private InvoiceResponse saveRoomFeeInvoice(Contract contract, YearMonth month) {
        LocalDate start = contract.getStartDate().toLocalDate();
        boolean firstMonth = YearMonth.from(start).equals(month);
        BigDecimal rent = firstMonth
                ? calculateFirstMonthRent(contract.getMonthlyRent(), start)
                : contract.getMonthlyRent().setScale(0, java.math.RoundingMode.HALF_UP);
        Invoice invoice = newMonthlyInvoice(InvoiceType.ROOM_FEE, month, rent);
        invoice.setContract(contract);
        String description = firstMonth ? "Tiền phòng tháng đầu (tính từ " + start + ")" : "Room rent";
        attachDetail(invoice, newDetail(InvoiceItemType.ROOM, description, BigDecimal.ONE, rent, rent));
        try {
            Invoice saved = invoiceRepository.saveAndFlush(invoice);
            notifyInvoiceCreated(saved);
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
    public InvoicePageResponse list(String month, int page, int size, String search, InvoiceStatus status) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "page must be nonnegative and size must be between 1 and 100");
        }
        PageRequest pageable = PageRequest.of(page, size);
        Page<Invoice> invoices = invoiceRepository.findManagerInvoices(month == null ? null : parseMonth(month).atDay(1),
                com.dormhub.specification.SearchSpecifications.normalize(search), status == null ? "" : status.name(), pageable);
        List<InvoiceSummaryResponse> content = invoices.getContent().stream()
                .map(invoice -> invoiceMapper.toSummary(invoice, overdue(invoice)))
                .toList();
        return new InvoicePageResponse(content, page, size, invoices.getTotalElements(), invoices.getTotalPages());
    }

    // ===== Hóa đơn của sinh viên (chỉ của chính mình) =====

    @Transactional(readOnly = true)
    public InvoicePageResponse listMyInvoices(Long userId, String month, int page, int size, String search, InvoiceStatus status) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "page phải >= 0 và size phải từ 1 đến 100");
        }
        PageRequest pageable = PageRequest.of(page, size);
        Page<Invoice> invoices = invoiceRepository.findAccessibleInvoices(userId,
                month == null ? null : parseMonth(month).atDay(1),
                com.dormhub.specification.SearchSpecifications.normalize(search), status == null ? "" : status.name(), pageable);
        List<InvoiceSummaryResponse> content = invoices.getContent().stream()
                .map(invoice -> invoiceMapper.toSummary(invoice, overdue(invoice)))
                .toList();
        return new InvoicePageResponse(content, page, size, invoices.getTotalElements(), invoices.getTotalPages());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getMyInvoice(Long userId, Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "invoiceId phải là số dương");
        }
        Invoice invoice = invoiceRepository.findAccessibleInvoice(invoiceId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND, "Không tìm thấy hóa đơn của bạn"));
        return invoiceMapper.toResponse(invoice, overdue(invoice));
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
