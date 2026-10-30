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
import com.dormhub.entity.Contract;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.enums.ReadingStatus;
import com.dormhub.entity.Room;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.InvoiceMapper;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.MeterReadingRepository;
import com.dormhub.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final RoomMonthlyChargeCalculator calculator;
    private final InvoiceMapper invoiceMapper;

    public InvoiceService(InvoiceRepository invoiceRepository, RoomRepository roomRepository,
            ContractRepository contractRepository,
            MeterReadingRepository meterReadingRepository, RoomMonthlyChargeCalculator calculator,
            InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.contractRepository = contractRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.calculator = calculator;
        this.invoiceMapper = invoiceMapper;
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
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND, "Room does not exist"));
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
        attachDetail(invoice, electricity);
        attachDetail(invoice, water);
        try {
            Invoice saved = invoiceRepository.saveAndFlush(invoice);
            return invoiceMapper.toResponse(saved, overdue(saved));
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Invoice violates a database constraint or already exists");
        }
    }

    private InvoiceResponse createRoomFee(InvoiceCreateRequest request, YearMonth month) {
        if (request.getContractId() == null || request.getContractId() <= 0 || request.getRoomId() != null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "ROOM_FEE requires a positive contractId and no roomId");
        }
        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new AppException(ErrorCode.BILLING_NOT_FOUND, "Contract does not exist"));
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
        if (invoiceRepository.existsByContract_ContractIdAndBillingMonth(request.getContractId(), month)) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "ROOM_FEE invoice already exists for this contract and month");
        }
        BigDecimal rent = contract.getMonthlyRent().setScale(0, java.math.RoundingMode.HALF_UP);
        Invoice invoice = newMonthlyInvoice(InvoiceType.ROOM_FEE, month, rent);
        invoice.setContract(contract);
        attachDetail(invoice, newDetail(InvoiceItemType.ROOM, "Room rent", BigDecimal.ONE, rent, rent));
        try {
            Invoice saved = invoiceRepository.saveAndFlush(invoice);
            return invoiceMapper.toResponse(saved, overdue(saved));
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.BILLING_CONFLICT, "Invoice violates a database constraint or already exists");
        }
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
