package com.dormhub.service;

import static com.dormhub.specification.SearchSpecifications.*;

import com.dormhub.dto.request.CheckoutCreateRequest;
import com.dormhub.dto.request.CheckoutCompleteRequest;
import com.dormhub.dto.request.CheckoutReviewRequest;
import com.dormhub.dto.response.CheckoutResponse;
import com.dormhub.entity.CheckoutRequest;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.CheckoutRequestStatus;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.entity.enums.InvoiceStatus;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.CheckoutMapper;
import com.dormhub.repository.*;

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
    private final RoomAssignmentService residenceService;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<CheckoutRequestStatus> OPEN_REQUEST_STATUSES =
            List.of(CheckoutRequestStatus.PENDING, CheckoutRequestStatus.APPROVED);

    private final CheckoutRequestRepository checkoutRequestRepository;
    private final ContractRepository contractRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final CheckInOutService checkInOutService;
    private final CheckoutMapper checkoutMapper;
    private final NotificationService notificationService;

    public CheckoutService(CheckoutRequestRepository checkoutRequestRepository,
                           ContractRepository contractRepository,
                           RoomAssignmentRepository roomAssignmentRepository,
                           RoomRepository roomRepository,
                           StudentRepository studentRepository,
                           InvoiceRepository invoiceRepository,
                           UserRepository userRepository,
                           CheckInOutService checkInOutService,
                           CheckoutMapper checkoutMapper,
                           NotificationService notificationService, RoomAssignmentService residenceService) {
        this.residenceService = residenceService;
        this.checkoutRequestRepository = checkoutRequestRepository;
        this.contractRepository = contractRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.checkInOutService = checkInOutService;
        this.checkoutMapper = checkoutMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public CheckoutResponse create(Long userId, CheckoutCreateRequest request) {
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        RoomAssignment assignment = residenceService.lockResidenceChange(userId, true);
        Student student = assignment.getStudent();
        Contract contract = assignment.getContract();
        residenceService.ensureNoOpenResidenceChange(student.getId(), null, null, null);

        CheckoutRequest checkoutRequest = new CheckoutRequest();
        checkoutRequest.setStudent(student);
        checkoutRequest.setContract(contract);
        checkoutRequest.setRequestDate(LocalDateTime.now(BUSINESS_ZONE));
        checkoutRequest.setStatus(CheckoutRequestStatus.PENDING);
        checkoutRequest.setReason(request.getReason().trim());
        CheckoutRequest saved = checkoutRequestRepository.save(checkoutRequest);
        notificationService.notifyResidenceRequestManagers("Yêu cầu trả phòng mới",
                "Sinh viên " + student.getStudentCode() + " gửi yêu cầu trả phòng #" + saved.getCheckoutRequestId() + ".");
        return checkoutMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CheckoutResponse> getMyRequests(Long userId, int page, int size, CheckoutRequestStatus status) {
        validatePage(page, size);
        Student student = getStudentByUserId(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDate"));
        return checkoutRequestRepository.findAll(
                com.dormhub.specification.SearchSpecifications.<CheckoutRequest>equal("student.id", student.getId())
                        .and(equal("status", status)), pageable).map(checkoutMapper::toResponse);
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
            CheckoutRequestStatus status, int page, int size, String search) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDate"));
        Page<CheckoutRequest> requests = checkoutRequestRepository.findAll(
                com.dormhub.specification.SearchSpecifications.<CheckoutRequest>text(search, "checkoutRequestId", "student.studentCode", "student.fullName", "student.user.fullName", "contract.room.roomNumber", "reason", "note").and(equal("status", status)), pageable);
        return requests.map(checkoutMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CheckoutResponse getRequestForManager(Long checkoutRequestId) {
        return checkoutMapper.toResponse(checkoutRequestRepository.findById(checkoutRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_REQUEST_NOT_FOUND)));
    }

    @Transactional
    public CheckoutResponse review(Long checkoutRequestId, CheckoutReviewRequest request, Long reviewerUserId) {
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
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getStatus() == CheckoutRequestStatus.APPROVED) {
            residenceService.lockResidenceChange(checkoutRequest.getStudent().getUser().getId(), true);
            residenceService.ensureNoOpenResidenceChange(checkoutRequest.getStudent().getId(), null, null, checkoutRequestId);
            validateCurrentStayForReview(checkoutRequest);
            validateNoUnpaidInvoices(checkoutRequest.getContract().getContractId());
        }

        checkoutRequest.setStatus(request.getStatus());
        checkoutRequest.setProcessedAt(LocalDateTime.now(BUSINESS_ZONE));
        checkoutRequest.setNote(trimToNull(request.getNote()));
        checkoutRequest.setReviewedBy(reviewer);
        CheckoutRequest savedRequest = checkoutRequestRepository.save(checkoutRequest);
        notifyReviewResult(savedRequest);
        return checkoutMapper.toResponse(savedRequest);
    }

    @Transactional
    public CheckoutResponse complete(Long checkoutRequestId, CheckoutCompleteRequest request, Long reviewerUserId) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        CheckoutRequest checkoutRequest = getRequestForUpdate(checkoutRequestId);
        if (checkoutRequest.getStatus() != CheckoutRequestStatus.APPROVED) {
            throw new AppException(ErrorCode.CHECKOUT_REQUEST_INVALID_STATUS);
        }

        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Long contractId = checkoutRequest.getContract().getContractId();

        // Khóa hợp đồng TRƯỚC khi kiểm tra hóa đơn unpaid (chống TOCTOU):
        // đường tạo hóa đơn ROOM_FEE cũng phải khóa hợp đồng (lockContract)
        // trước, nên không thể phát hành hóa đơn mới chen ngang giữa lúc
        // kiểm tra nợ và lúc chốt trả phòng.
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        residenceService.lockResidenceChange(contract.getStudent().getUser().getId(), true);
        residenceService.ensureNoOpenResidenceChange(contract.getStudent().getId(), null, null, checkoutRequestId);
        validateCurrentStayForReview(checkoutRequest);
        validateNoUnpaidInvoicesUnderLock(contract);
        validateDepositDisposition(contract, request.getDepositDisposition());
        if (request.getDepositDisposition() == DepositStatus.FORFEITED
                && (request.getNote() == null || request.getNote().isBlank())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Cần ghi rõ căn cứ xử lý không hoàn tiền cọc.");
        }
        if (request.getNote() != null && !request.getNote().isBlank()) {
            String note = (checkoutRequest.getNote() == null ? "" : checkoutRequest.getNote() + "\n") + request.getNote().trim();
            if (note.length() > 1000) throw new AppException(ErrorCode.INVALID_REQUEST, "Tổng ghi chú tối đa 1000 ký tự.");
            checkoutRequest.setNote(note);
        }

        // Quyết định cọc TRƯỚC khi checkOut để đường trả phòng duy nhất:
        // checkOut từ chối hợp đồng còn cọc HELD (không cho rơi cọc lại trạng thái HELD)
        if (contract.getDepositStatus() == DepositStatus.HELD) {
            contract.setDepositStatus(request.getDepositDisposition());
            contractRepository.save(contract);
        }

        checkInOutService.checkOut(contractId);

        checkoutRequest.setStatus(CheckoutRequestStatus.COMPLETED);
        checkoutRequest.setCheckoutDate(LocalDate.now(BUSINESS_ZONE));
        checkoutRequest.setProcessedAt(LocalDateTime.now(BUSINESS_ZONE));
        checkoutRequest.setReviewedBy(reviewer);
        return checkoutMapper.toResponse(checkoutRequestRepository.save(checkoutRequest));
    }

    /** Manager hủy yêu cầu APPROVED bị kẹt (SV đổi ý) — chỉ khi chưa hoàn tất trả phòng. */
    @Transactional
    public CheckoutResponse cancelForManager(Long checkoutRequestId, CheckoutReviewRequest request, Long reviewerUserId) {
        if (request == null || request.getNote() == null || request.getNote().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Cần ghi rõ lý do khi hủy yêu cầu trả phòng");
        }
        CheckoutRequest checkoutRequest = getRequestForUpdate(checkoutRequestId);
        if (checkoutRequest.getStatus() != CheckoutRequestStatus.PENDING
                && checkoutRequest.getStatus() != CheckoutRequestStatus.APPROVED) {
            throw new AppException(ErrorCode.CHECKOUT_REQUEST_INVALID_STATUS,
                    "Chỉ có thể hủy yêu cầu trả phòng đang PENDING hoặc APPROVED");
        }
        checkoutRequest.setReviewedBy(userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
        checkoutRequest.setStatus(CheckoutRequestStatus.CANCELLED);
        checkoutRequest.setProcessedAt(LocalDateTime.now(BUSINESS_ZONE));
        checkoutRequest.setNote(trimToNull(request.getNote()));
        notificationService.sendNotification(NotificationType.GENERAL, "Yêu cầu trả phòng đã được hủy",
                "Lý do: " + checkoutRequest.getNote(), checkoutRequestId, checkoutRequest.getStudent().getUser().getId());
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

    /** Kiểm tra nợ dạng tra cứu nhanh (dùng cho bước review — bước chốt là complete()). */
    private void validateNoUnpaidInvoices(Long contractId) {
        Contract contract = contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
        residenceService.ensureNoUnpaidResidenceInvoices(contract);
    }

    /**
     * Kiểm tra nợ khi ĐÃ khóa hợp đồng; khóa thêm phòng hiện tại để hóa đơn
     * UTILITY mới (tự khóa phòng qua lockRoom) không thể chen ngang giữa lúc
     * kiểm tra và lúc chốt trả phòng (chống TOCTOU). Thứ tự khóa: hợp đồng →
     * phân phòng → phòng, nhất quán với luồng chuyển phòng.
     */
    private void validateNoUnpaidInvoicesUnderLock(Contract contract) {
        residenceService.lockResidenceInvoiceRooms(contract.getStudent().getId(), contract.getRoom().getId());
        residenceService.ensureNoUnpaidResidenceInvoices(contract);
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
        notificationService.sendNotification(NotificationType.CHECKOUT_CONFIRMED, title, body,
                checkoutRequest.getCheckoutRequestId(), studentUser.getId());
    }

    /** Khóa biên (PESSIMISTIC_WRITE) phân phòng CHECKED_IN mới nhất của SV. */
    private RoomAssignment getCurrentAssignmentForUpdate(Long studentId) {
        return roomAssignmentRepository
                .findAllByStudent_IdAndStatusOrderByAssignmentIdDescForUpdate(
                        studentId, RoomAssignmentStatus.CHECKED_IN)
                .stream()
                .findFirst()
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
