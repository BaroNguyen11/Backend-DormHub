package com.dormhub.service;

import com.dormhub.dto.response.ContractResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.ContractMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContractService {

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

    public ContractService(ContractRepository contractRepository,
                           RoomRepository roomRepository,
                           StudentRepository studentRepository,
                           InvoiceRepository invoiceRepository,
                           ContractMapper contractMapper,
                           DepositService depositService,
                           NotificationService notificationService) {
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.invoiceRepository = invoiceRepository;
        this.contractMapper = contractMapper;
        this.depositService = depositService;
        this.notificationService = notificationService;
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
        LocalDateTime startDate = application.getProcessedAt() == null
                ? LocalDateTime.now(BUSINESS_ZONE)
                : application.getProcessedAt();

        Contract contract = new Contract();
        contract.setDormApplication(application);
        contract.setRoom(room);
        contract.setStudent(student);
        contract.setStartDate(startDate);
        contract.setEndDate(startDate.plusMonths(12));
        contract.setMonthlyRent(monthlyRent);
        contract.setDepositAmount(monthlyRent);
        contract.setDepositStatus(DepositStatus.UNPAID);
        contract.setStatus(ContractStatus.PENDING);
        contract.setDescription("Hợp đồng nội trú 12 tháng từ đơn " + application.getId());
        Contract saved = contractRepository.saveAndFlush(contract);

        long heldAfterSave = contractRepository.countByRoom_IdAndStatusIn(
                room.getId(), ROOM_HOLD_STATUSES);
        room.setStatus(room.getCurrentOccupancy() + heldAfterSave >= room.getCapacity()
                ? RoomStatus.RESERVED
                : RoomStatus.AVAILABLE);
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
        return contractMapper.toResponse(findContract(contractId));
    }

    public Page<ContractResponse> search(Long studentId, ContractStatus status, Pageable pageable) {
        Page<Contract> contracts;
        if (studentId != null && status != null) {
            contracts = contractRepository.findAllByStudent_IdAndStatus(studentId, status, pageable);
        } else if (studentId != null) {
            contracts = contractRepository.findAllByStudent_Id(studentId, pageable);
        } else if (status != null) {
            contracts = contractRepository.findAllByStatus(status, pageable);
        } else {
            contracts = contractRepository.findAll(pageable);
        }
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
        long holdsLeft = contractRepository.countByRoom_IdAndStatusIn(room.getId(), ROOM_HOLD_STATUSES);
        if (room.getStatus() != RoomStatus.MAINTENANCE) {
            room.setStatus(room.getCurrentOccupancy() + holdsLeft >= room.getCapacity()
                    ? RoomStatus.RESERVED
                    : room.getCurrentOccupancy() >= room.getCapacity()
                            ? RoomStatus.OCCUPIED
                            : RoomStatus.AVAILABLE);
        }
        roomRepository.save(room);
        return contractMapper.toResponse(contract);
    }

    private Contract findContract(Long contractId) {
        if (contractId == null || contractId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "contractId phải là số dương");
        }
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
    }
}
