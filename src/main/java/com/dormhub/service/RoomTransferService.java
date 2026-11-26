package com.dormhub.service;

import static com.dormhub.specification.SearchSpecifications.*;

import com.dormhub.dto.request.RoomTransferCreateRequest;
import com.dormhub.dto.request.RoomTransferReviewRequest;
import com.dormhub.dto.response.RoomTransferResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.RoomTransferRequest;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.TransferStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomTransferMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.RoomTransferRequestRepository;
import com.dormhub.repository.StudentRepository;
import com.dormhub.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomTransferService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<RoomAssignmentStatus> OCCUPYING_ASSIGNMENT_STATUSES =
            List.of(RoomAssignmentStatus.ASSIGNED, RoomAssignmentStatus.CHECKED_IN);

    private final RoomTransferRequestRepository transferRequestRepository;
    private final ContractRepository contractRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomTransferMapper roomTransferMapper;
    private final NotificationService notificationService;
    private final RoomStatusService roomStatusService;

    public RoomTransferService(RoomTransferRequestRepository transferRequestRepository,
                               ContractRepository contractRepository,
                               RoomAssignmentRepository roomAssignmentRepository,
                               RoomRepository roomRepository,
                               StudentRepository studentRepository,
                               UserRepository userRepository,
                               InvoiceRepository invoiceRepository,
                               RoomTransferMapper roomTransferMapper,
                               NotificationService notificationService,
                               RoomStatusService roomStatusService) {
        this.transferRequestRepository = transferRequestRepository;
        this.contractRepository = contractRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomTransferMapper = roomTransferMapper;
        this.notificationService = notificationService;
        this.roomStatusService = roomStatusService;
    }

    @Transactional
    public RoomTransferResponse create(RoomTransferCreateRequest request, Long userId) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Student student = getStudentByUserId(userId);
        // Khóa biên phân phòng hiện tại (PESSIMISTIC_WRITE): hai request tạo
        // chuyển phòng đồng thời sẽ tuần tự hóa trên dòng này, nên lần kiểm
        // tra "đã có request PENDING" bên dưới không thể bị hai luồng vượt qua
        // cùng lúc (chặn tạo trùng yêu cầu chuyển phòng).
        RoomAssignment currentAssignment = getCurrentAssignmentForStudentForUpdate(student.getId());
        ensureStudentIsCheckedIn(student, currentAssignment);

        if (transferRequestRepository.existsByStudent_IdAndStatus(
                student.getId(), TransferStatus.PENDING)) {
            throw new AppException(ErrorCode.TRANSFER_ALREADY_PENDING);
        }

        Room preferredRoom = null;
        if (request.getPreferredRoomId() != null) {
            if (Objects.equals(request.getPreferredRoomId(), currentAssignment.getRoom().getId())) {
                throw new AppException(ErrorCode.TRANSFER_SAME_ROOM);
            }
            preferredRoom = roomRepository.findById(request.getPreferredRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        }

        RoomTransferRequest transferRequest = new RoomTransferRequest();
        transferRequest.setStudent(student);
        transferRequest.setCurrentAssignment(currentAssignment);
        transferRequest.setPreferredRoom(preferredRoom);
        transferRequest.setReason(trimToNull(request.getReason()));
        transferRequest.setStatus(TransferStatus.PENDING);
        transferRequest.setRequestedAt(LocalDateTime.now(BUSINESS_ZONE));

        return roomTransferMapper.toResponse(transferRequestRepository.save(transferRequest));
    }

    @Transactional(readOnly = true)
    public Page<RoomTransferResponse> getMyRequests(Long userId, int page, int size) {
        validatePage(page, size);
        Student student = getStudentByUserId(userId);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        return transferRequestRepository.findAllByStudent_Id(student.getId(), pageable)
                .map(roomTransferMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RoomTransferResponse getMyRequest(Long transferId, Long userId) {
        Student student = getStudentByUserId(userId);
        RoomTransferRequest transferRequest = transferRequestRepository
                .findByTransferIdAndStudent_Id(transferId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        return roomTransferMapper.toResponse(transferRequest);
    }

    @Transactional
    public RoomTransferResponse cancel(Long transferId, Long userId) {
        Student student = getStudentByUserId(userId);
        RoomTransferRequest transferRequest = getTransferRequestForUpdate(transferId);
        ensureStudentOwnsRequest(transferRequest, student.getId());
        ensurePending(transferRequest);

        transferRequest.setStatus(TransferStatus.CANCELLED);
        return roomTransferMapper.toResponse(transferRequest);
    }

    @Transactional(readOnly = true)
    public Page<RoomTransferResponse> getRequestsForManager(
            TransferStatus status, int page, int size, String search) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<RoomTransferRequest> requests = transferRequestRepository.findAll(
                com.dormhub.specification.SearchSpecifications.<RoomTransferRequest>text(search, "transferId", "student.studentCode", "student.fullName", "student.user.fullName", "currentAssignment.room.roomNumber", "preferredRoom.roomNumber", "approvedRoom.roomNumber", "reason", "reviewNote").and(equal("status", status)), pageable);
        return requests.map(roomTransferMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RoomTransferResponse getRequestForManager(Long transferId) {
        RoomTransferRequest transferRequest = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        return roomTransferMapper.toResponse(transferRequest);
    }

    @Transactional
    public RoomTransferResponse review(Long transferId, RoomTransferReviewRequest request,
                                       Long reviewerUserId) {
        if (request == null || request.getStatus() == null
                || (request.getStatus() != TransferStatus.APPROVED
                && request.getStatus() != TransferStatus.REJECTED)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Kết quả xử lý chỉ được là APPROVED hoặc REJECTED");
        }

        RoomTransferRequest transferRequest = getTransferRequestForUpdate(transferId);
        ensurePending(transferRequest);
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getStatus() == TransferStatus.APPROVED) {
            approveTransfer(transferRequest, request, reviewer);
        }

        transferRequest.setStatus(request.getStatus());
        transferRequest.setReviewedBy(reviewer);
        transferRequest.setReviewedAt(LocalDateTime.now(BUSINESS_ZONE));
        transferRequest.setReviewNote(trimToNull(request.getReviewNote()));
        RoomTransferRequest savedRequest = transferRequestRepository.save(transferRequest);
        notifyStudent(savedRequest);
        return roomTransferMapper.toResponse(savedRequest);
    }

    /**
     * Thứ tự khóa cố định của luồng duyệt chuyển phòng:
     * HỢP ĐỒNG → PHÂN PHÒNG → PHÒNG (khóa theo ID phòng tăng dần).
     * Khóa hợp đồng và phòng hiện tại TRƯỚC khi kiểm tra hóa đơn unpaid để
     * chặn TOCTOU: hóa đơn ROOM_FEE mới (tự khóa hợp đồng qua lockContract)
     * và hóa đơn UTILITY mới (tự khóa phòng qua lockRoom) không thể chen ngang
     * giữa lúc kiểm tra nợ và lúc chốt chuyển phòng.
     */
    private void approveTransfer(RoomTransferRequest transferRequest,
                                 RoomTransferReviewRequest request, User reviewer) {
        // (1) Khóa hợp đồng TRƯỚC: đường tạo hóa đơn ROOM_FEE cũng phải khóa
        // hợp đồng trước, nên không thể phát hành hóa đơn mới trong lúc duyệt.
        Long contractId = transferRequest.getCurrentAssignment().getContract().getContractId();
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        // (2) Đọc lại dữ liệu MỚI của phân phòng (khóa biên), không dùng snapshot
        // chụp lúc tạo request: nếu SV đã đổi phòng sau khi tạo request thì phân
        // phòng cũ sẽ không còn CHECKED_IN và bị từ chối ngay ở ensureStudentIsCheckedIn.
        Long currentAssignmentId = transferRequest.getCurrentAssignment().getAssignmentId();
        RoomAssignment currentAssignment = roomAssignmentRepository
                .findByAssignmentIdForUpdate(currentAssignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
        ensureStudentIsCheckedIn(transferRequest.getStudent(), currentAssignment);

        Long destinationRoomId = request.getApprovedRoomId() != null
                ? request.getApprovedRoomId()
                : transferRequest.getPreferredRoom() == null
                ? null : transferRequest.getPreferredRoom().getId();
        if (destinationRoomId == null || destinationRoomId <= 0) {
            throw new AppException(ErrorCode.TRANSFER_ROOM_REQUIRED);
        }
        // So sánh với phòng hiện tại theo dữ liệu MỚI vừa khóa (bước 2).
        if (Objects.equals(destinationRoomId, currentAssignment.getRoom().getId())) {
            throw new AppException(ErrorCode.TRANSFER_SAME_ROOM);
        }

        // (3) Khóa CẢ HAI phòng theo ID tăng dần để tránh deadlock khi hai yêu
        // cầu chuyển phòng ngược chiều (A: X→Y, B: Y→X) chạy đồng thời.
        long currentRoomId = currentAssignment.getRoom().getId();
        long firstRoomId = Math.min(currentRoomId, destinationRoomId);
        long secondRoomId = Math.max(currentRoomId, destinationRoomId);
        Room firstRoom = roomRepository.findByIdForUpdate(firstRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Room secondRoom = secondRoomId == firstRoomId ? firstRoom
                : roomRepository.findByIdForUpdate(secondRoomId)
                        .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Room currentRoom = currentRoomId == firstRoomId ? firstRoom : secondRoom;
        Room destinationRoom = destinationRoomId == firstRoomId ? firstRoom : secondRoom;

        // (4) Kiểm tra nợ SAU KHI đã khóa hợp đồng + phòng hiện tại.
        ensureNoUnpaidInvoices(contract, currentRoom);

        validateDestinationRoom(transferRequest.getStudent(), destinationRoom);

        // Quy tắc giá khi chuyển phòng (BR §13.4): chỉ cho chuyển sang phòng cùng mức giá
        // để tránh thay đổi monthlyRent/depositAmount âm thầm trong hợp đồng đang ACTIVE.
        if (contract.getMonthlyRent() == null
                || destinationRoom.getPricePerMonth() == null
                || destinationRoom.getPricePerMonth().compareTo(contract.getMonthlyRent()) != 0) {
            throw new AppException(ErrorCode.TRANSFER_PRICE_MISMATCH,
                    "Chỉ được chuyển sang phòng cùng mức giá với hợp đồng hiện tại");
        }

        LocalDateTime transferTime = LocalDateTime.now(BUSINESS_ZONE);
        currentAssignment.setStatus(RoomAssignmentStatus.CHECKED_OUT);
        currentAssignment.setCheckOutAt(transferTime);
        roomAssignmentRepository.save(currentAssignment);

        RoomAssignment newAssignment = new RoomAssignment();
        newAssignment.setStudent(transferRequest.getStudent());
        newAssignment.setContract(currentAssignment.getContract());
        newAssignment.setRoom(destinationRoom);
        newAssignment.setAssignedBy(reviewer);
        newAssignment.setAssignedAt(transferTime);
        newAssignment.setCheckInAt(transferTime);
        newAssignment.setStatus(RoomAssignmentStatus.CHECKED_IN);
        roomAssignmentRepository.save(newAssignment);
        roomAssignmentRepository.flush();

        // Đồng bộ 2 phòng qua hàm recompute chuẩn duy nhất (phân biệt đúng RESERVED/OCCUPIED)
        roomStatusService.syncOccupancyAndRefreshStatus(currentAssignment.getRoom().getId());
        roomStatusService.syncOccupancyAndRefreshStatus(destinationRoom.getId());

        contract.setRoom(destinationRoom);
        contract.setMonthlyRent(destinationRoom.getPricePerMonth());
        contractRepository.save(contract);
        Student student = transferRequest.getStudent();
        student.setRoom(destinationRoom);
        studentRepository.save(student);
        transferRequest.setApprovedRoom(destinationRoom);
    }

    /** Gọi SAU KHI đã khóa hợp đồng và phòng hiện tại — xem approveTransfer. */
    private void ensureNoUnpaidInvoices(Contract contract, Room currentRoom) {
        if (invoiceRepository.existsByContract_ContractIdAndStatus(
                contract.getContractId(), InvoiceStatus.UNPAID)) {
            throw new AppException(ErrorCode.CONTRACT_HAS_UNPAID_INVOICES,
                    "Không thể chuyển phòng khi hợp đồng còn hóa đơn chưa thanh toán");
        }
        if (currentRoom != null && invoiceRepository.existsByRoom_IdAndStatusAndInvoiceType(
                currentRoom.getId(), InvoiceStatus.UNPAID, InvoiceType.UTILITY)) {
            throw new AppException(ErrorCode.CONTRACT_HAS_UNPAID_INVOICES,
                    "Không thể chuyển phòng khi phòng hiện tại còn hóa đơn tiện ích chưa thanh toán");
        }
    }

    private void validateDestinationRoom(Student student, Room destinationRoom) {
        long assignmentOccupancy = roomAssignmentRepository.countByRoom_IdAndStatusIn(
                destinationRoom.getId(), OCCUPYING_ASSIGNMENT_STATUSES);
        int occupiedSlots = Math.max(destinationRoom.getCurrentOccupancy(),
                Math.toIntExact(assignmentOccupancy));
        if (destinationRoom.getStatus() != RoomStatus.AVAILABLE
                || destinationRoom.getCapacity() == null
                || occupiedSlots >= destinationRoom.getCapacity()) {
            throw new AppException(ErrorCode.TRANSFER_ROOM_NOT_AVAILABLE);
        }

        if (destinationRoom.getPricePerMonth() == null || destinationRoom.getPricePerMonth().signum() <= 0) {
            throw new AppException(ErrorCode.TRANSFER_ROOM_NOT_AVAILABLE, "Phòng chuyển đến chưa có giá hợp lệ");
        }

        Gender studentGender = student.getGender();
        Gender roomRestriction = destinationRoom.getBuilding().getGenderRestriction();
        if (roomRestriction == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        if (studentGender != Gender.MALE && studentGender != Gender.FEMALE) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }
        if (studentGender != roomRestriction) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
    }

    private void notifyStudent(RoomTransferRequest transferRequest) {
        User studentUser = transferRequest.getStudent().getUser();
        if (studentUser == null) {
            return;
        }

        boolean approved = transferRequest.getStatus() == TransferStatus.APPROVED;
        String title = approved
                ? "Yêu cầu chuyển phòng đã được duyệt"
                : "Yêu cầu chuyển phòng đã bị từ chối";
        String body = approved
                ? "Yêu cầu chuyển phòng của bạn đã được duyệt."
                : "Yêu cầu chuyển phòng của bạn đã bị từ chối.";
        if (transferRequest.getReviewNote() != null) {
            body += " Ghi chú: " + transferRequest.getReviewNote();
        }
        notificationService.sendNotification(NotificationType.TRANSFER_PROCESSED,
                title, body, transferRequest.getTransferId(), studentUser.getId());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }

    private void ensureStudentIsCheckedIn(Student student, RoomAssignment assignment) {
        if (!Objects.equals(assignment.getStudent().getId(), student.getId())) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND);
        }
        if (assignment.getStatus() != RoomAssignmentStatus.CHECKED_IN
                || assignment.getContract().getStatus() != ContractStatus.ACTIVE) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS,
                    "Chỉ sinh viên đang CHECKED_IN theo hợp đồng ACTIVE mới được chuyển phòng");
        }
    }

    /** Khóa biên (PESSIMISTIC_WRITE) phân phòng CHECKED_IN mới nhất của SV. */
    private RoomAssignment getCurrentAssignmentForStudentForUpdate(Long studentId) {
        return roomAssignmentRepository
                .findAllByStudent_IdAndStatusOrderByAssignmentIdDescForUpdate(
                        studentId, RoomAssignmentStatus.CHECKED_IN)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
    }

    private Student getStudentByUserId(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    private RoomTransferRequest getTransferRequestForUpdate(Long transferId) {
        if (transferId == null || transferId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return transferRequestRepository.findByTransferIdForUpdate(transferId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
    }

    private void ensureStudentOwnsRequest(RoomTransferRequest request, Long studentId) {
        if (!Objects.equals(request.getStudent().getId(), studentId)) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND);
        }
    }

    private void ensurePending(RoomTransferRequest request) {
        if (request.getStatus() != TransferStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSFER_INVALID_STATUS);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
