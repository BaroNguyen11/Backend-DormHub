package com.dormhub.service.impl;

import com.dormhub.dto.request.InvoiceCreateRequest;
import com.dormhub.dto.response.InvoiceDetailResponse;
import com.dormhub.dto.response.InvoicePageResponse;
import com.dormhub.dto.response.InvoiceResponse;
import com.dormhub.dto.response.InvoiceSummaryResponse;
import com.dormhub.dto.response.RoomMonthlyChargeResult;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.InvoiceDetail;
import com.dormhub.entity.InvoiceItemType;
import com.dormhub.entity.InvoiceType;
import com.dormhub.entity.InvoiceStatus;
import com.dormhub.entity.Contract;
import com.dormhub.entity.ContractStatus;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.ReadingStatus;
import com.dormhub.entity.Room;
import com.dormhub.exception.InvoiceConflictException;
import com.dormhub.exception.InvoiceNotFoundException;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.MeterReadingRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.service.InvoiceService;
import com.dormhub.service.RoomMonthlyChargeCalculator;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final RoomMonthlyChargeCalculator calculator;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, RoomRepository roomRepository,
            ContractRepository contractRepository,
            MeterReadingRepository meterReadingRepository, RoomMonthlyChargeCalculator calculator) {
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.contractRepository = contractRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.calculator = calculator;
    }

    @Override
    @Transactional
    public InvoiceResponse create(InvoiceCreateRequest request) {
        if (request == null || request.getInvoiceType() == null || request.getInvoiceType().isBlank()) {
            throw new IllegalArgumentException("invoiceType is required");
        }
        InvoiceType type = parseType(request.getInvoiceType());
        if (type == InvoiceType.DEPOSIT) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                    "DEPOSIT invoice creation is not supported in this phase");
        }
        YearMonth month = parseMonth(request.getBillingMonth());
        if (type == InvoiceType.ROOM_FEE) {
            return createRoomFee(request, month);
        }
        if (request.getContractId() != null) {
            throw new IllegalArgumentException("contractId is only valid for ROOM_FEE");
        }
        if (request.getRoomId() == null || request.getRoomId() <= 0) {
            throw new IllegalArgumentException("A positive roomId is required");
        }
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new InvoiceNotFoundException("Room does not exist"));
        MeterReading reading = meterReadingRepository
                .findByRoom_RoomIdAndReadingMonth(request.getRoomId(), month);
        if (reading == null) {
            throw new InvoiceNotFoundException("Meter reading does not exist");
        }
        if (reading.getStatus() != ReadingStatus.CONFIRMED) {
            throw new InvoiceConflictException("Meter reading is not confirmed");
        }
        if (invoiceRepository.existsByRoom_RoomIdAndBillingMonth(request.getRoomId(), month)) {
            throw new InvoiceConflictException("UTILITY invoice already exists for this room and month");
        }

        RoomMonthlyChargeResult charge;
        try {
            charge = calculator.calculate(request.getRoomId(), month, reading);
        } catch (IllegalArgumentException exception) {
            throw new InvoiceConflictException(exception.getMessage());
        }

        InvoiceDetail electricity = new InvoiceDetail(
                InvoiceItemType.ELECTRICITY, "Electricity", charge.electricityConsumption(),
                reading.getElectricityPrice(), charge.electricityAmount());
        InvoiceDetail water = new InvoiceDetail(
                InvoiceItemType.WATER, "Water", charge.waterConsumption(),
                reading.getWaterPrice(), charge.waterAmount());
        BigDecimal total = electricity.getAmount().add(water.getAmount());

        Invoice invoice = new Invoice(room, month, LocalDateTime.now(BUSINESS_ZONE), total);
        invoice.addDetail(electricity);
        invoice.addDetail(water);
        try {
            return toResponse(invoiceRepository.saveAndFlush(invoice));
        } catch (DataIntegrityViolationException exception) {
            throw new InvoiceConflictException("Invoice violates a database constraint or already exists");
        }
    }

    private InvoiceResponse createRoomFee(InvoiceCreateRequest request, YearMonth month) {
        if (request.getContractId() == null || request.getContractId() <= 0 || request.getRoomId() != null) {
            throw new IllegalArgumentException("ROOM_FEE requires a positive contractId and no roomId");
        }
        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new InvoiceNotFoundException("Contract does not exist"));
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        if (contract.getStatus() != ContractStatus.ACTIVE
                || contract.getStartDate().toLocalDate().isAfter(last)
                || contract.getEndDate().toLocalDate().isBefore(first)) {
            throw new InvoiceConflictException("Contract is not active in the billing month");
        }
        if (contract.getMonthlyRent() == null || contract.getMonthlyRent().signum() < 0) {
            throw new InvoiceConflictException("Contract monthly rent is invalid");
        }
        if (invoiceRepository.existsByContract_ContractIdAndBillingMonth(request.getContractId(), month)) {
            throw new InvoiceConflictException("ROOM_FEE invoice already exists for this contract and month");
        }
        BigDecimal rent = contract.getMonthlyRent().setScale(0, java.math.RoundingMode.HALF_UP);
        Invoice invoice = new Invoice(contract, month, LocalDateTime.now(BUSINESS_ZONE), rent);
        invoice.addDetail(new InvoiceDetail(InvoiceItemType.ROOM, "Room rent", BigDecimal.ONE, rent, rent));
        try {
            return toResponse(invoiceRepository.saveAndFlush(invoice));
        } catch (DataIntegrityViolationException exception) {
            throw new InvoiceConflictException("Invoice violates a database constraint or already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InvoicePageResponse list(String month, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be nonnegative and size must be between 1 and 100");
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invoiceId"));
        Page<Invoice> invoices = month == null
                ? invoiceRepository.findAll(pageable)
                : invoiceRepository.findByBillingMonth(parseMonth(month), pageable);
        List<InvoiceSummaryResponse> content = invoices.getContent().stream().map(this::toSummary).toList();
        return new InvoicePageResponse(content, page, size, invoices.getTotalElements(), invoices.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse get(Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A positive invoiceId is required");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice does not exist"));
        return toResponse(invoice);
    }

    private InvoiceType parseType(String value) {
        try {
            return InvoiceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("invoiceType must be UTILITY, ROOM_FEE or DEPOSIT", exception);
        }
    }

    private YearMonth parseMonth(String value) {
        if (value == null) {
            throw new IllegalArgumentException("billingMonth is required");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("billingMonth must use YYYY-MM", exception);
        }
    }

    private InvoiceSummaryResponse toSummary(Invoice invoice) {
        return new InvoiceSummaryResponse(
                invoice.getInvoiceId(), invoice.getInvoiceType(), roomId(invoice), contractId(invoice),
                invoice.getBillingMonth(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getStatus(), overdue(invoice), invoice.getTotalAmount());
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceDetailResponse> details = invoice.getDetails().stream()
                .map(detail -> new InvoiceDetailResponse(
                        detail.getDetailId(), detail.getItemType(), detail.getDescription(),
                        detail.getQuantity(), detail.getUnitPrice(), detail.getAmount()))
                .toList();
        return new InvoiceResponse(
                invoice.getInvoiceId(), invoice.getInvoiceType(), roomId(invoice), contractId(invoice),
                invoice.getBillingMonth(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getStatus(), overdue(invoice), invoice.getTotalAmount(), details);
    }

    private Long roomId(Invoice invoice) {
        return invoice.getRoom() != null ? invoice.getRoom().getRoomId()
                : invoice.getContract().getRoom().getRoomId();
    }

    private Long contractId(Invoice invoice) {
        return invoice.getContract() == null ? null : invoice.getContract().getContractId();
    }

    private boolean overdue(Invoice invoice) {
        return invoice.getDueDate() != null && invoice.getStatus() == InvoiceStatus.UNPAID
                && invoice.getDueDate().toLocalDate().isBefore(LocalDate.now(BUSINESS_ZONE));
    }
}
