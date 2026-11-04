package com.dormhub.service;

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
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.TransferStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomTransferMapper;
import com.dormhub.repository.ContractRepository;
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
    private final RoomTransferMapper roomTransferMapper;
    private final NotificationService notificationService;

    public RoomTransferService(RoomTransferRequestRepository transferRequestRepository,
                               ContractRepository contractRepository,
                               RoomAssignmentRepository roomAssignmentRepository,
                               RoomRepository roomRepository,
                               StudentRepository studentRepository,
                               UserRepository userRepository,
                               RoomTransferMapper roomTransferMapper,
                               NotificationService notificationService) {
        this.transferRequestRepository = transferRequestRepository;
        this.contractRepository = contractRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.roomTransferMapper = roomTransferMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public RoomTransferResponse create(RoomTransferCreateRequest request, Long userId) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Student student = getStudentByUserId(userId);
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
            TransferStatus status, int page, int size) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<RoomTransferRequest> requests = status == null
                ? transferRequestRepository.findAll(pageable)
                : transferRequestRepository.findAllByStatus(status, pageable);
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

    private void approveTransfer(RoomTransferRequest transferRequest,
                                 RoomTransferReviewRequest request, User reviewer) {
        Long contractId = transferRequest.getCurrentAssignment().getContract().getContractId();
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
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
        if (Objects.equals(destinationRoomId, currentAssignment.getRoom().getId())) {
            throw new AppException(ErrorCode.TRANSFER_SAME_ROOM);
        }

        Room destinationRoom = roomRepository.findByIdForUpdate(destinationRoomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        validateDestinationRoom(transferRequest.getStudent(), contract, destinationRoom);

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

        contract.setRoom(destinationRoom);
        transferRequest.setApprovedRoom(destinationRoom);
    }

    private void validateDestinationRoom(Student student, Contract contract,
                                         Room destinationRoom) {
        long assignmentOccupancy = roomAssignmentRepository.countByRoom_IdAndStatusIn(
                destinationRoom.getId(), OCCUPYING_ASSIGNMENT_STATUSES);
        int occupiedSlots = Math.max(destinationRoom.getCurrentOccupancy(),
                Math.toIntExact(assignmentOccupancy));
        if (destinationRoom.getStatus() != RoomStatus.AVAILABLE
                || destinationRoom.getCapacity() == null
                || occupiedSlots >= destinationRoom.getCapacity()) {
            throw new AppException(ErrorCode.TRANSFER_ROOM_NOT_AVAILABLE);
        }

        if (contract.getMonthlyRent() == null || destinationRoom.getPricePerMonth() == null
                || contract.getMonthlyRent().compareTo(destinationRoom.getPricePerMonth()) != 0) {
            throw new AppException(ErrorCode.TRANSFER_PRICE_MISMATCH);
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

    private RoomAssignment getCurrentAssignmentForStudentForUpdate(Long studentId) {
        return roomAssignmentRepository
                .findFirstByStudent_IdAndStatusOrderByAssignmentIdDesc(
                        studentId, RoomAssignmentStatus.CHECKED_IN)
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
