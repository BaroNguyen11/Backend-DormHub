package com.dormhub.service;

import com.dormhub.dto.request.CheckoutCreateRequest;
import com.dormhub.dto.request.CheckoutCompleteRequest;
import com.dormhub.dto.request.CheckoutReviewRequest;
import com.dormhub.dto.response.CheckoutResponse;
import com.dormhub.entity.CheckoutRequest;
import com.dormhub.entity.Contract;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.CheckoutRequestStatus;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.CheckoutMapper;
import com.dormhub.repository.CheckoutRequestRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.StudentRepository;
import java.time.LocalDate;
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
public class CheckoutService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<CheckoutRequestStatus> OPEN_REQUEST_STATUSES =
            List.of(CheckoutRequestStatus.PENDING, CheckoutRequestStatus.APPROVED);

    private final CheckoutRequestRepository checkoutRequestRepository;
    private final ContractRepository contractRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final StudentRepository studentRepository;
    private final CheckInOutService checkInOutService;
    private final CheckoutMapper checkoutMapper;
    private final NotificationService notificationService;

    public CheckoutService(CheckoutRequestRepository checkoutRequestRepository,
                           ContractRepository contractRepository,
                           RoomAssignmentRepository roomAssignmentRepository,
                           StudentRepository studentRepository,
                           CheckInOutService checkInOutService,
                           CheckoutMapper checkoutMapper,
                           NotificationService notificationService) {
        this.checkoutRequestRepository = checkoutRequestRepository;
        this.contractRepository = contractRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.studentRepository = studentRepository;
        this.checkInOutService = checkInOutService;
        this.checkoutMapper = checkoutMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public CheckoutResponse create(Long userId, CheckoutCreateRequest request) {
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Student student = getStudentByUserId(userId);
        RoomAssignment assignment = getCurrentAssignmentForUpdate(student.getId());
        ensureStudentIsCheckedIn(student, assignment);
        Contract contract = assignment.getContract();

        if (checkoutRequestRepository.existsByContract_ContractIdAndStatusIn(
                contract.getContractId(), OPEN_REQUEST_STATUSES)) {
            throw new AppException(ErrorCode.CHECKOUT_REQUEST_ALREADY_OPEN);
        }

        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setStudent(student);
        checkoutRequest.setContract(contract);
        checkoutRequest.setRequestDate(LocalDateTime.now(BUSINESS_ZONE));
        checkoutRequest.setStatus(CheckoutRequestStatus.PENDING);
        checkoutRequest.setReason(request.getReason().trim());
        return checkoutMapper.toResponse(checkoutRequestRepository.save(checkoutRequest));
    }

    @Transactional(readOnly = true)
    public Page<CheckoutResponse> getMyRequests(Long userId, int page, int size) {
        validatePage(page, size);
        Student student = getStudentByUserId(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDate"));
        return checkoutRequestRepository.findAllByStudent_Id(student.getId(), pageable)
                .map(checkoutMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CheckoutResponse getMyRequest(Long checkoutRequestId, Long userId) {
        Student student = getStudentByUserId(userId);
        CheckoutRequest checkoutRequest = checkoutRequestRepository
                .findByCheckoutRequestIdAndStudent_Id(checkoutRequestId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_REQUEST_NOT_FOUND));
        return checkoutMapper.toResponse(checkoutRequest);
    }

    @Transactional
    public CheckoutResponse cancel(Long checkoutRequestId, Long userId) {
        Student student = getStudentByUserId(userId);
        CheckoutRequest checkoutRequest = getRequestForUpdate(checkoutRequestId);
        ensureStudentOwnsRequest(checkoutRequest, student.getId());
        ensurePending(checkoutRequest);
        checkoutRequest.setStatus(CheckoutRequestStatus.CANCELLED);
        return checkoutMapper.toResponse(checkoutRequestRepository.save(checkoutRequest));
    }

    @Transactional(readOnly = true)
    public Page<CheckoutResponse> getRequestsForManager(
            CheckoutRequestStatus status, int page, int size) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDate"));
        Page<CheckoutRequest> requests = status == null
                ? checkoutRequestRepository.findAll(pageable)
                : checkoutRequestRepository.findAllByStatus(status, pageable);
        return requests.map(checkoutMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CheckoutResponse getRequestForManager(Long checkoutRequestId) {
        return checkoutMapper.toResponse(checkoutRequestRepository.findById(checkoutRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_REQUEST_NOT_FOUND)));
    }

    @Transactional
    public CheckoutResponse review(Long checkoutRequestId, CheckoutReviewRequest request) {
        if (request == null || request.getStatus() == null
                || (request.getStatus() != CheckoutRequestStatus.APPROVED
                && request.getStatus() != CheckoutRequestStatus.REJECTED)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Kết quả xử lý chỉ được là APPROVED hoặc REJECTED");
        }
        if (request.getStatus() == CheckoutRequestStatus.REJECTED
                && (request.getNote() == null || request.getNote().isBlank())) {
            throw new AppException(ErrorCode.CHECKOUT_REJECTION_NOTE_REQUIRED);
        }

        CheckoutRequest checkoutRequest = getRequestForUpdate(checkoutRequestId);
        ensurePending(checkoutRequest);

        if (request.getStatus() == CheckoutRequestStatus.APPROVED) {
            validateCurrentStayForReview(checkoutRequest);
        }

        checkoutRequest.setStatus(request.getStatus());
        checkoutRequest.setProcessedAt(LocalDateTime.now(BUSINESS_ZONE));
        checkoutRequest.setNote(trimToNull(request.getNote()));
        CheckoutRequest savedRequest = checkoutRequestRepository.save(checkoutRequest);
        notifyReviewResult(savedRequest);
        return checkoutMapper.toResponse(savedRequest);
    }

    @Transactional
    public CheckoutResponse complete(Long checkoutRequestId, CheckoutCompleteRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        CheckoutRequest checkoutRequest = getRequestForUpdate(checkoutRequestId);
        if (checkoutRequest.getStatus() != CheckoutRequestStatus.APPROVED) {
            throw new AppException(ErrorCode.CHECKOUT_REQUEST_INVALID_STATUS);
        }

        Long contractId = checkoutRequest.getContract().getContractId();
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        validateDepositDisposition(contract, request.getDepositDisposition());

        checkInOutService.checkOut(contractId);
        if (contract.getDepositStatus() == DepositStatus.HELD) {
            contract.setDepositStatus(request.getDepositDisposition());
        }

        checkoutRequest.setStatus(CheckoutRequestStatus.COMPLETED);
        checkoutRequest.setCheckoutDate(LocalDate.now(BUSINESS_ZONE));
        checkoutRequest.setProcessedAt(LocalDateTime.now(BUSINESS_ZONE));
        return checkoutMapper.toResponse(checkoutRequestRepository.save(checkoutRequest));
    }

    private void validateDepositDisposition(Contract contract, DepositStatus disposition) {
        if (contract.getDepositStatus() == DepositStatus.HELD) {
            if (disposition == null) {
                throw new AppException(ErrorCode.CHECKOUT_DEPOSIT_ACTION_REQUIRED);
            }
            if (disposition != DepositStatus.REFUNDED && disposition != DepositStatus.FORFEITED) {
                throw new AppException(ErrorCode.CHECKOUT_DEPOSIT_ACTION_INVALID);
            }
        } else if (disposition != null) {
            throw new AppException(ErrorCode.CHECKOUT_DEPOSIT_NOT_HELD);
        }
    }

    private void validateCurrentStayForReview(CheckoutRequest checkoutRequest) {
        Contract contract = contractRepository
                .findByContractIdForUpdate(checkoutRequest.getContract().getContractId())
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        RoomAssignment assignment = roomAssignmentRepository
                .findFirstByContract_ContractIdOrderByAssignmentIdDesc(contract.getContractId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));

        if (!Objects.equals(assignment.getContract().getContractId(), contract.getContractId())
                || !Objects.equals(assignment.getStudent().getId(),
                checkoutRequest.getStudent().getId())) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS);
        }
        if (assignment.getStatus() != RoomAssignmentStatus.CHECKED_IN
                || (contract.getStatus() != ContractStatus.ACTIVE
                && contract.getStatus() != ContractStatus.EXPIRED)) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS);
        }
    }

    private void notifyReviewResult(CheckoutRequest checkoutRequest) {
        User studentUser = checkoutRequest.getStudent().getUser();
        if (studentUser == null) {
            return;
        }

        boolean approved = checkoutRequest.getStatus() == CheckoutRequestStatus.APPROVED;
        String title = approved
                ? "Yêu cầu trả phòng đã được duyệt"
                : "Yêu cầu trả phòng đã bị từ chối";
        String body = approved
                ? "Yêu cầu trả phòng của bạn đã được duyệt. Hãy phối hợp với quản lý để xác nhận trả phòng."
                : "Yêu cầu trả phòng của bạn đã bị từ chối.";
        if (checkoutRequest.getNote() != null) {
            body += " Ghi chú: " + checkoutRequest.getNote();
        }
        notificationService.sendNotification(NotificationType.GENERAL, title, body,
                checkoutRequest.getCheckoutRequestId(), studentUser.getId());
    }

    private RoomAssignment getCurrentAssignmentForUpdate(Long studentId) {
        return roomAssignmentRepository
                .findFirstByStudent_IdAndStatusOrderByAssignmentIdDesc(
                        studentId, RoomAssignmentStatus.CHECKED_IN)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND));
    }

    private void ensureStudentIsCheckedIn(Student student, RoomAssignment assignment) {
        if (!Objects.equals(assignment.getStudent().getId(), student.getId())
                || !Objects.equals(assignment.getContract().getStudent().getId(), student.getId())) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_NOT_FOUND);
        }
        ContractStatus status = assignment.getContract().getStatus();
        if (assignment.getStatus() != RoomAssignmentStatus.CHECKED_IN
                || (status != ContractStatus.ACTIVE && status != ContractStatus.EXPIRED)) {
            throw new AppException(ErrorCode.ROOM_ASSIGNMENT_INVALID_STATUS);
        }
    }

    private Student getStudentByUserId(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    private CheckoutRequest getRequestForUpdate(Long checkoutRequestId) {
        if (checkoutRequestId == null || checkoutRequestId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return checkoutRequestRepository.findByCheckoutRequestIdForUpdate(checkoutRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_REQUEST_NOT_FOUND));
    }

    private void ensureStudentOwnsRequest(CheckoutRequest checkoutRequest, Long studentId) {
        if (!Objects.equals(checkoutRequest.getStudent().getId(), studentId)) {
            throw new AppException(ErrorCode.CHECKOUT_REQUEST_NOT_FOUND);
        }
    }

    private void ensurePending(CheckoutRequest checkoutRequest) {
        if (checkoutRequest.getStatus() != CheckoutRequestStatus.PENDING) {
            throw new AppException(ErrorCode.CHECKOUT_REQUEST_INVALID_STATUS);
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
