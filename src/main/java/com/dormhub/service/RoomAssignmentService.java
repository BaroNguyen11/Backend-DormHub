package com.dormhub.service;

import com.dormhub.dto.response.RoomAssignmentResponse;
import com.dormhub.dto.response.RoommateResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Contract;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomAssignmentMapper;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomAssignmentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<RoomAssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            List.of(RoomAssignmentStatus.ASSIGNED, RoomAssignmentStatus.CHECKED_IN);
    private static final List<ContractStatus> ROOM_HOLD_STATUSES = List.of(ContractStatus.PENDING);

    private final RoomAssignmentRepository roomAssignmentRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final StudentRepository studentRepository;
    private final RoomAssignmentMapper roomAssignmentMapper;
    private final RoomStatusService roomStatusService;
    private final NotificationService notificationService;
    public RoomAssignmentService(RoomAssignmentRepository roomAssignmentRepository,
                                 ContractRepository contractRepository,
                                 InvoiceRepository invoiceRepository,
                                 RoomRepository roomRepository,
                                 BuildingRepository buildingRepository,
                                 StudentRepository studentRepository,
                                 RoomAssignmentMapper roomAssignmentMapper,
                                 RoomStatusService roomStatusService, NotificationService notificationService) {
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.buildingRepository = buildingRepository;
        this.studentRepository = studentRepository;
        this.roomAssignmentMapper = roomAssignmentMapper;
        this.roomStatusService = roomStatusService;
        this.notificationService = notificationService;
    }

    @Transactional
    public RoomAssignmentResponse assign(Long contractId) {
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        if (contract.getStatus() != ContractStatus.ACTIVE
                || contract.getDepositStatus() != DepositStatus.HELD) {
            throw new AppException(ErrorCode.CONTRACT_NOT_ACTIVE);
        }
        Invoice depositInvoice = invoiceRepository
                .findByContract_ContractIdAndInvoiceType(contractId, InvoiceType.DEPOSIT)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID));
        if (depositInvoice.getStatus() != InvoiceStatus.PAID) {
            throw new AppException(ErrorCode.CONTRACT_DEPOSIT_NOT_PAID);
        }

        DormApplication application = contract.getDormApplication();
        if (application == null || application.getStatus() != DormApplicationStatus.APPROVED) {
            throw new AppException(ErrorCode.DORM_APPLICATION_NOT_APPROVED);
        }
        User approver = application.getProcessedBy();
        if (approver == null || approver.getId() == null) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_APPROVER_REQUIRED);
        }
        Student student = studentRepository.findByUserIdForUpdate(contract.getStudent().getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        if (roomAssignmentRepository.existsByStudent_IdAndStatusIn(
                student.getId(), ACTIVE_ASSIGNMENT_STATUSES)) {
            throw new AppException(ErrorCode.STUDENT_ALREADY_ASSIGNED);
        }
        if (roomAssignmentRepository.existsByContract_ContractIdAndStatusIn(
                contractId, ACTIVE_ASSIGNMENT_STATUSES)) {
            throw new AppException(ErrorCode.DORM_APPLICATION_ALREADY_ASSIGNED);
        }

        Room room = roomRepository.findByIdForUpdate(contract.getRoom().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Building building = buildingRepository.findByIdForUpdate(room.getBuilding().getId())
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        ensureGenderMatches(student.getGender(), building.getGenderRestriction());

        long otherPendingHolds = contractRepository.countByRoom_IdAndStatusIn(
                room.getId(), ROOM_HOLD_STATUSES);
        if (room.getCurrentOccupancy() + otherPendingHolds >= room.getCapacity()) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }

        RoomAssignment assignment = new RoomAssignment();
        assignment.setStudent(student);
        assignment.setContract(contract);
        assignment.setRoom(room);
        assignment.setAssignedBy(approver);
        assignment.setStatus(RoomAssignmentStatus.ASSIGNED);
        RoomAssignment savedAssignment = roomAssignmentRepository.saveAndFlush(assignment);
        student.setRoom(room);
        studentRepository.save(student);

        // Đồng bộ occupancy + trạng thái phòng qua hàm chuẩn duy nhất (P0)
        roomStatusService.syncOccupancyAndRefreshStatus(room.getId());

        if (student.getUser() != null) {
            notificationService.sendNotification(
                    NotificationType.ROOM_ASSIGNED,
                    "Đã được phân phòng",
                    String.format("Bạn đã được phân vào phòng %s, tòa %s. Vui lòng đến văn phòng KTX để nhận phòng.",
                            room.getRoomNumber(), room.getBuilding().getName()),
                    contract.getContractId(),
                    student.getUser().getId());
        }
        return roomAssignmentMapper.toResponse(savedAssignment);
    }

    public Page<RoomAssignmentResponse> getMyAssignments(Long userId, int page, int size) {
        validatePage(page, size);
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        return roomAssignmentRepository.findAllByStudent_Id(student.getId(), pageable(page, size))
                .map(roomAssignmentMapper::toResponse);
    }

    public List<RoommateResponse> getMyRoommates(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        return roomAssignmentRepository.findCurrentResidences(
                        student.getId(), ACTIVE_ASSIGNMENT_STATUSES, ContractStatus.ACTIVE, now)
                .stream()
                .findFirst()
                .map(assignment -> roomAssignmentRepository.findCurrentRoommates(
                        assignment.getRoom().getId(), student.getId(),
                        ACTIVE_ASSIGNMENT_STATUSES, ContractStatus.ACTIVE, now))
                .orElseGet(List::of);
    }

    public Page<RoomAssignmentResponse> getAll(RoomAssignmentStatus status, int page, int size) {
        validatePage(page, size);
        Pageable pageable = pageable(page, size);
        Page<RoomAssignment> assignments = status == null
                ? roomAssignmentRepository.findAll(pageable)
                : roomAssignmentRepository.findAllByStatus(status, pageable);
        return assignments.map(roomAssignmentMapper::toResponse);
    }

    public RoomAssignmentResponse getMyAssignment(Long userId, Long assignmentId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        return roomAssignmentRepository.findByAssignmentIdAndStudent_Id(assignmentId, student.getId())
                .map(roomAssignmentMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
    }

    public RoomAssignmentResponse getById(Long assignmentId) {
        return roomAssignmentRepository.findByAssignmentId(assignmentId)
                .map(roomAssignmentMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
    }

    private void ensureGenderMatches(Gender studentGender, Gender restriction) {
        if (restriction == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        if ((studentGender != Gender.MALE && studentGender != Gender.FEMALE)
                || studentGender != restriction) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }
}
