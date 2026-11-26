package com.dormhub.service;

import static com.dormhub.specification.SearchSpecifications.*;

import com.dormhub.dto.response.ContractResponse;
import com.dormhub.dto.response.CheckInPlanResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Payment;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.PaymentStatus;
import com.dormhub.entity.enums.SepayTransactionStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.ContractMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.PaymentRepository;
import com.dormhub.repository.SepayTransactionRepository;
import com.dormhub.repository.StudentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContractService {
    public static final int RENTAL_DURATION_MONTHS = 12;
    public static final long DEPOSIT_RECONCILIATION_GRACE_HOURS = 1;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<ContractStatus> OPEN_CONTRACT_STATUSES =
            List.of(ContractStatus.PENDING, ContractStatus.ACTIVE);
    private static final List<ContractStatus> ROOM_HOLD_STATUSES = List.of(ContractStatus.PENDING);

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContractMapper contractMapper;
    private final DepositService depositService;
    private final NotificationService notificationService;
    private final RoomStatusService roomStatusService;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final InvoiceService invoiceService;
    private final PaymentRepository paymentRepository;
    private final SepayTransactionRepository sepayTransactionRepository;

    public ContractService(ContractRepository contractRepository,
                           RoomRepository roomRepository,
                           StudentRepository studentRepository,
                           InvoiceRepository invoiceRepository,
                           ContractMapper contractMapper,
                           DepositService depositService,
                           NotificationService notificationService,
                           RoomStatusService roomStatusService,
                           RoomAssignmentRepository roomAssignmentRepository,
                           InvoiceService invoiceService,
                           PaymentRepository paymentRepository,
                           SepayTransactionRepository sepayTransactionRepository) {
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.invoiceRepository = invoiceRepository;
        this.contractMapper = contractMapper;
        this.depositService = depositService;
        this.notificationService = notificationService;
        this.roomStatusService = roomStatusService;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.invoiceService = invoiceService;
        this.paymentRepository = paymentRepository;
        this.sepayTransactionRepository = sepayTransactionRepository;
    }

    @Transactional
    public Contract createPendingForApprovedApplication(DormApplication application) {
        if (application.getStatus() != DormApplicationStatus.APPROVED) {
            throw new AppException(ErrorCode.DORM_APPLICATION_NOT_APPROVED);
        }
        if (contractRepository.findByDormApplication_Id(application.getId()).isPresent()) {
            throw new AppException(ErrorCode.CONTRACT_ALREADY_EXISTS);
        }

        Student requestedStudent = application.getStudent();
        if (requestedStudent.getUser() == null || requestedStudent.getUser().getId() == null) {
            throw new AppException(ErrorCode.STUDENT_NOT_FOUND);
        }
        Student student = studentRepository.findByUserIdForUpdate(requestedStudent.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        if (student.getGender() != Gender.MALE && student.getGender() != Gender.FEMALE) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }
        if (contractRepository.existsByStudent_IdAndStatusIn(student.getId(), OPEN_CONTRACT_STATUSES)) {
            throw new AppException(ErrorCode.CONTRACT_CONFLICT,
                    "Sinh viên đã có hợp đồng PENDING hoặc ACTIVE");
        }

        Long requestedRoomId = application.getRequestedRoom().getId();
        Room room = roomRepository.findByIdForUpdate(requestedRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getBuilding().getGenderRestriction() == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        if (room.getBuilding().getGenderRestriction() != student.getGender()) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }
        long alreadyHeld = contractRepository.countByRoom_IdAndStatusIn(
                room.getId(), ROOM_HOLD_STATUSES);
        if (room.getCurrentOccupancy() + alreadyHeld >= room.getCapacity()) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }

        BigDecimal roomPrice = room.getPricePerMonth();
        if (roomPrice == null || roomPrice.signum() <= 0) {
            throw new AppException(ErrorCode.CONTRACT_RENT_INVALID);
        }
        BigDecimal monthlyRent = roomPrice.setScale(0, RoundingMode.HALF_UP);
        Contract contract = new Contract();
        contract.setDormApplication(application);
        contract.setRoom(room);
        contract.setStudent(student);
        LocalDate startDate = application.getProcessedAt().toLocalDate();
        YearMonth endMonth = YearMonth.from(startDate)
                .plusMonths(startDate.getDayOfMonth() <= 15 ? 11 : 12);
        contract.setStartDate(startDate.atStartOfDay());
        contract.setEndDate(endMonth.atEndOfMonth().atTime(23, 59, 59));
        contract.setMonthlyRent(monthlyRent);
        contract.setDepositAmount(monthlyRent);
        contract.setDepositStatus(DepositStatus.UNPAID);
        contract.setStatus(ContractStatus.PENDING);
        contract.setDescription("Hợp đồng nội trú 12 tháng từ đơn " + application.getId());
        Contract saved = contractRepository.saveAndFlush(contract);

        roomStatusService.refreshStatus(room.getId());
        roomRepository.save(room);

        depositService.createDepositInvoice(saved.getContractId());
        if (student.getUser() != null) {
            notificationService.sendNotification(
                    NotificationType.CONTRACT_CREATED,
                    "Hợp đồng mới",
                    "Hợp đồng đặt phòng đã được tạo; vui lòng thanh toán hóa đơn đặt cọc",
                    saved.getContractId(),
                    student.getUser().getId());
        }
        return saved;
    }

    public ContractResponse getById(Long contractId) {
        ContractResponse response = contractMapper.toResponse(findContract(contractId));
        roomAssignmentRepository.findTopByContract_ContractIdOrderByAssignmentIdDesc(contractId)
                .ifPresent(assignment -> response.setAssignmentStatus(assignment.getStatus()));
        return response;
    }

    public Page<ContractResponse> getMyContracts(Long userId, ContractStatus status, Pageable pageable) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        if (status != null) {
            return contractRepository.findAllByStudent_IdAndStatus(student.getId(), status, pageable)
                    .map(contractMapper::toResponse);
        }
        return contractRepository.findAllByStudent_Id(student.getId(), pageable)
                .map(contractMapper::toResponse);
    }

    public ContractResponse getMyContract(Long userId, Long contractId) {
        return contractMapper.toResponse(getMyContractEntity(userId, contractId));
    }

    public Contract getMyContractEntity(Long userId, Long contractId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        Contract contract = findContract(contractId);
        if (!Objects.equals(contract.getStudent().getId(), student.getId())) {
            throw new AppException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        return contract;
    }

    public CheckInPlanResponse previewCheckIn(Long userId, Long contractId, LocalDate requestedDate) {
        Contract contract = getMyContractEntity(userId, contractId);
        Payment payment = successfulDepositPayment(contract);
        LocalDate paidDate = payment.getPaidAt().atZone(BUSINESS_ZONE).toLocalDate();
        if (contract.getStatus() == ContractStatus.ACTIVE) {
            return toCheckInPlan(contract, payment, contract.getStartDate().toLocalDate(), true);
        }
        requireAwaitingDate(contract);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate selected = requestedDate == null
                ? (today.isAfter(paidDate) ? today : paidDate) : requestedDate;
        validateCheckInDate(paidDate, selected, today);
        return toCheckInPlan(contract, payment, selected, false);
    }

    @Transactional
    public ContractResponse confirmCheckIn(Long userId, Long contractId, LocalDate selectedDate) {
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        if (!Objects.equals(contract.getStudent().getUser().getId(), userId)) {
            throw new AppException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        if (selectedDate == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng chọn ngày nhận phòng");
        }
        if (contract.getStatus() == ContractStatus.ACTIVE) {
            if (contract.getStartDate().toLocalDate().equals(selectedDate)) {
                return contractMapper.toResponse(contract);
            }
            throw new AppException(ErrorCode.CONTRACT_CONFLICT, "Ngày nhận phòng đã được xác nhận");
        }
        requireAwaitingDate(contract);
        Payment payment = successfulDepositPayment(contract);
        LocalDate paidDate = payment.getPaidAt().atZone(BUSINESS_ZONE).toLocalDate();
        validateCheckInDate(paidDate, selectedDate, LocalDate.now(BUSINESS_ZONE));
        YearMonth endMonth = YearMonth.from(selectedDate)
                .plusMonths(selectedDate.getDayOfMonth() <= 15 ? 11 : 12);
        contract.setStartDate(selectedDate.atStartOfDay());
        contract.setEndDate(endMonth.atEndOfMonth().atTime(LocalTime.of(23, 59, 59)));
        contract.setStatus(ContractStatus.ACTIVE);
        contractRepository.saveAndFlush(contract);
        roomStatusService.refreshStatus(contract.getRoom().getId());
        invoiceService.createFirstMonthRoomFeeIfAbsent(contract);
        return contractMapper.toResponse(contract);
    }

    private void requireAwaitingDate(Contract contract) {
        if (contract.getStatus() != ContractStatus.PENDING
                || contract.getDepositStatus() != DepositStatus.HELD
                || !roomAssignmentRepository.existsByContract_ContractIdAndStatusIn(
                    contract.getContractId(), List.of(RoomAssignmentStatus.ASSIGNED))) {
            throw new AppException(ErrorCode.CONTRACT_CONFLICT,
                    "Hợp đồng chưa đủ điều kiện chọn ngày nhận phòng");
        }
    }

    private Payment successfulDepositPayment(Contract contract) {
        Invoice deposit = invoiceRepository.findByContract_ContractIdAndInvoiceType(
                contract.getContractId(), InvoiceType.DEPOSIT)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID));
        if (deposit.getStatus() != InvoiceStatus.PAID || contract.getDepositStatus() != DepositStatus.HELD) {
            throw new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID);
        }
        Payment payment = paymentRepository.findFirstByInvoice_InvoiceIdAndStatusOrderByCreatedAtDesc(
                deposit.getInvoiceId(), PaymentStatus.SUCCESS)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID));
        if (payment.getPaidAt() == null) {
            throw new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID);
        }
        return payment;
    }

    private void validateCheckInDate(LocalDate paidDate, LocalDate selected, LocalDate today) {
        if (selected.isBefore(paidDate) || selected.isBefore(today)
                || selected.isAfter(paidDate.plusDays(7))) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Ngày nhận phòng phải từ hôm nay đến hết 7 ngày sau khi đóng cọc");
        }
    }

    private CheckInPlanResponse toCheckInPlan(Contract contract, Payment payment,
            LocalDate selected, boolean confirmed) {
        LocalDate paidDate = payment.getPaidAt().atZone(BUSINESS_ZONE).toLocalDate();
        YearMonth endMonth = YearMonth.from(selected)
                .plusMonths(selected.getDayOfMonth() <= 15 ? 11 : 12);
        LocalDate endDate = confirmed ? contract.getEndDate().toLocalDate() : endMonth.atEndOfMonth();
        return new CheckInPlanResponse(contract.getContractId(), contract.getRoom().getRoomNumber(),
                contract.getRoom().getBuilding().getName(),
                payment.getPaidAt().atZone(BUSINESS_ZONE).toLocalDateTime(),
                paidDate, paidDate.plusDays(7), selected, selected, endDate,
                contract.getMonthlyRent(), InvoiceService.calculateFirstMonthRent(
                        contract.getMonthlyRent(), selected), confirmed);
    }

    public Page<ContractResponse> search(Long studentId, ContractStatus status, Pageable pageable, String search) {
        var spec = com.dormhub.specification.SearchSpecifications.<Contract>text(search,
                "contractId", "student.studentCode", "student.fullName", "student.user.fullName", "room.roomNumber", "room.building.name")
                .and(equal("student.id", studentId)).and(equal("status", status));
        Page<Contract> contracts = contractRepository.findAll(spec, pageable);
        return contracts.map(contractMapper::toResponse);
    }

    @Transactional
    public ContractResponse cancelPending(Long contractId) {
        if (contractId == null || contractId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "contractId phải là số dương");
        }
        Invoice depositInvoice = invoiceRepository.findByContract_ContractIdAndInvoiceTypeForUpdate(
                        contractId, InvoiceType.DEPOSIT)
                .orElseThrow(() -> new AppException(ErrorCode.DEPOSIT_CONFLICT,
                        "Không tìm thấy hóa đơn cọc của hợp đồng"));
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        if (contract.getStatus() != ContractStatus.PENDING) {
            throw new AppException(ErrorCode.CONTRACT_CONFLICT, "Chỉ có thể hủy hợp đồng PENDING");
        }
        if (depositInvoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new AppException(ErrorCode.DEPOSIT_CONFLICT,
                    "Chỉ có thể hủy hợp đồng khi hóa đơn cọc chưa thanh toán");
        }

        Room room = roomRepository.findByIdForUpdate(contract.getRoom().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        depositInvoice.setStatus(InvoiceStatus.CANCELLED);
        contract.cancelPending();
        contractRepository.saveAndFlush(contract);
        if (contract.getDormApplication() != null) {
            contract.getDormApplication().setStatus(DormApplicationStatus.CANCELLED);
        }
        roomStatusService.refreshStatus(room.getId());
        roomRepository.save(room);
        return contractMapper.toResponse(contract);
    }

    @Transactional
    public void expireUnpaidDeposit(Long invoiceId) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId).orElse(null);
        if (invoice == null || invoice.getInvoiceType() != InvoiceType.DEPOSIT
                || invoice.getContract() == null || invoice.getDueDate() == null) {
            return;
        }
        Contract contract = contractRepository.findByContractIdForUpdate(invoice.getContract().getContractId())
                .orElse(null);
        if (contract == null || contract.getStatus() != ContractStatus.PENDING
                || contract.getDepositStatus() != DepositStatus.UNPAID
                || invoice.getStatus() != InvoiceStatus.UNPAID
                || !invoice.getDueDate().plusHours(DEPOSIT_RECONCILIATION_GRACE_HOURS)
                    .isBefore(LocalDateTime.now(BUSINESS_ZONE))) {
            return;
        }
        for (var payment : paymentRepository.findAllByInvoice_InvoiceIdOrderByCreatedAtDesc(invoiceId)) {
            if (payment.getStatus() == PaymentStatus.SUCCESS
                    || (payment.getStatus() == PaymentStatus.PENDING
                        && payment.getExpiresAt().isAfter(java.time.Instant.now()))
                    || sepayTransactionRepository.existsByCodeAndStatusIn(payment.getPaymentReference(),
                        List.of(SepayTransactionStatus.UNMATCHED, SepayTransactionStatus.REVIEW_REQUIRED))) {
                return;
            }
        }
        cancelPending(contract.getContractId());
        notificationService.sendNotification(NotificationType.GENERAL,
                "Đã hết hạn đóng cọc", "Suất phòng đã được giải phóng vì quá hạn thanh toán tiền cọc.",
                contract.getContractId(), contract.getStudent().getUser().getId());
    }

    private Contract findContract(Long contractId) {
        if (contractId == null || contractId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "contractId phải là số dương");
        }
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
    }
}
