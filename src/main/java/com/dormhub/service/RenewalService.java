package com.dormhub.service;

import static com.dormhub.specification.SearchSpecifications.*;

import com.dormhub.dto.request.RenewalCreateRequest;
import com.dormhub.dto.request.RenewalReviewRequest;
import com.dormhub.dto.response.RenewalResponse;
import com.dormhub.entity.CheckoutRequest;
import com.dormhub.entity.Contract;
import com.dormhub.entity.RenewalRequest;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.CheckoutRequestStatus;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RenewalStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RenewalMapper;
import com.dormhub.repository.CheckoutRequestRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.RenewalRequestRepository;
import com.dormhub.repository.RoomAssignmentRepository;
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
public class RenewalService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_PAGE_SIZE = 100;
    private final RoomAssignmentService residenceService;

    private final ContractRepository contractRepository;
    private final RenewalRequestRepository renewalRequestRepository;
    private final CheckoutRequestRepository checkoutRequestRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RenewalMapper renewalMapper;
    private final NotificationService notificationService;
    private final RoomAssignmentRepository roomAssignmentRepository;

    public RenewalService(ContractRepository contractRepository,
                          RenewalRequestRepository renewalRequestRepository,
                          CheckoutRequestRepository checkoutRequestRepository,
                          StudentRepository studentRepository,
                          UserRepository userRepository,
                          RenewalMapper renewalMapper,
                          NotificationService notificationService,
                          RoomAssignmentRepository roomAssignmentRepository, RoomAssignmentService residenceService) {
        this.residenceService = residenceService;
        this.contractRepository = contractRepository;
        this.renewalRequestRepository = renewalRequestRepository;
        this.checkoutRequestRepository = checkoutRequestRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.renewalMapper = renewalMapper;
        this.notificationService = notificationService;
        this.roomAssignmentRepository = roomAssignmentRepository;
    }

    @Transactional
    public RenewalResponse create(Long contractId, RenewalCreateRequest request, Long userId) {
        if (request == null || (request.getRequestedEndDate() == null && request.getMonths() == null)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        var assignment = residenceService.lockResidenceChange(userId, false);
        Student student = assignment.getStudent();
        Contract contract = assignment.getContract();
        if (!Objects.equals(contract.getContractId(), contractId)) throw new AppException(ErrorCode.CONTRACT_NOT_FOUND);
        residenceService.ensureNoOpenResidenceChange(student.getId(), null, null, null);
        ensureStudentOwnsContract(contract, student.getId());
        ensureActiveContract(contract);
        if (!roomAssignmentRepository.existsByContract_ContractIdAndStatusIn(
                contractId, List.of(RoomAssignmentStatus.CHECKED_IN))) {
            throw new AppException(ErrorCode.RENEWAL_INVALID_STATUS,
                    "Chỉ có thể gia hạn sau khi đã nhận phòng");
        }
        LocalDateTime requestedEnd = resolveRequestedEnd(contract, request);
        // Cross-validation: không cho nộp gia hạn khi còn yêu cầu trả phòng đang mở
        ensureNoOpenCheckoutRequest(contractId);

        if (renewalRequestRepository.existsByContract_ContractIdAndStatus(
                contractId, RenewalStatus.PENDING)) {
            throw new AppException(ErrorCode.RENEWAL_ALREADY_PENDING);
        }

        RenewalRequest renewalRequest = new RenewalRequest();
        renewalRequest.setContract(contract);
        renewalRequest.setRequestedEndDate(requestedEnd);
        renewalRequest.setReason(trimToNull(request.getReason()));
        renewalRequest.setStatus(RenewalStatus.PENDING);
        renewalRequest.setRequestedAt(LocalDateTime.now(BUSINESS_ZONE));

        RenewalRequest saved = renewalRequestRepository.save(renewalRequest);
        notificationService.notifyResidenceRequestManagers("Yêu cầu gia hạn hợp đồng mới",
                "Sinh viên " + student.getStudentCode() + " gửi yêu cầu gia hạn #" + saved.getRenewalRequestId() + ".");
        return renewalMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<RenewalResponse> getMyRequests(Long userId, int page, int size, RenewalStatus status) {
        validatePage(page, size);
        Student student = getStudentByUserId(userId);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        return renewalRequestRepository.findAll(
                com.dormhub.specification.SearchSpecifications.<RenewalRequest>equal("contract.student.id", student.getId())
                        .and(equal("status", status)), pageable).map(renewalMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RenewalResponse getMyRequest(Long renewalRequestId, Long userId) {
        Student student = getStudentByUserId(userId);
        RenewalRequest renewalRequest = renewalRequestRepository
                .findByRenewalRequestIdAndContract_Student_Id(renewalRequestId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RENEWAL_REQUEST_NOT_FOUND));
        return renewalMapper.toResponse(renewalRequest);
    }

    @Transactional
    public RenewalResponse cancel(Long renewalRequestId, Long userId) {
        Student student = getStudentByUserId(userId);
        RenewalRequest renewalRequest = getRenewalRequestForUpdate(renewalRequestId);
        ensureStudentOwnsRequest(renewalRequest, student.getId());
        ensurePendingRequest(renewalRequest);

        renewalRequest.setStatus(RenewalStatus.CANCELLED);
        return renewalMapper.toResponse(renewalRequest);
    }

    @Transactional(readOnly = true)
    public Page<RenewalResponse> getRequestsForManager(
            RenewalStatus status, int page, int size, String search) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<RenewalRequest> requests = renewalRequestRepository.findAll(
                com.dormhub.specification.SearchSpecifications.<RenewalRequest>text(search, "renewalRequestId", "contract.contractId", "contract.student.studentCode", "contract.student.fullName", "contract.student.user.fullName", "contract.room.roomNumber", "reason", "reviewNote").and(equal("status", status)), pageable);
        return requests.map(renewalMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RenewalResponse getRequestForManager(Long renewalRequestId) {
        RenewalRequest renewalRequest = renewalRequestRepository.findById(renewalRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RENEWAL_REQUEST_NOT_FOUND));
        return renewalMapper.toResponse(renewalRequest);
    }

    @Transactional
    public RenewalResponse review(Long renewalRequestId, RenewalReviewRequest request,
                                  Long reviewerUserId) {
        if (request == null || request.getStatus() == null
                || (request.getStatus() != RenewalStatus.APPROVED
                && request.getStatus() != RenewalStatus.REJECTED)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Kết quả duyệt chỉ được là APPROVED hoặc REJECTED");
        }

        RenewalRequest renewalRequest = getRenewalRequestForUpdate(renewalRequestId);
        ensurePendingRequest(renewalRequest);

        Contract contract = getContractForUpdate(renewalRequest.getContract().getContractId());
        if (request.getStatus() == RenewalStatus.APPROVED) {
            residenceService.lockResidenceChange(contract.getStudent().getUser().getId(), false);
            residenceService.ensureNoOpenResidenceChange(contract.getStudent().getId(), renewalRequestId, null, null);
            ensureActiveContract(contract);
            ensureRequestedEndDateIsAfterCurrent(contract, renewalRequest.getRequestedEndDate());
            // Cross-validation (plan Issue #16): chặn gia hạn khi còn yêu cầu trả phòng đang mở
            ensureNoOpenCheckoutRequest(contract.getContractId());
            contract.setEndDate(renewalRequest.getRequestedEndDate());
            contractRepository.save(contract);
        }

        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        // Đồng bộ quy ước: từ chối bắt buộc ghi lý do (như DormApplication/Checkout)
        if (request.getStatus() == RenewalStatus.REJECTED
                && (request.getReviewNote() == null || request.getReviewNote().isBlank())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Cần ghi rõ lý do khi từ chối gia hạn");
        }
        renewalRequest.setStatus(request.getStatus());
        renewalRequest.setReviewedBy(reviewer);
        renewalRequest.setReviewedAt(LocalDateTime.now(BUSINESS_ZONE));
        renewalRequest.setReviewNote(trimToNull(request.getReviewNote()));
        RenewalRequest savedRequest = renewalRequestRepository.save(renewalRequest);

        if (contract.getStudent() != null && contract.getStudent().getUser() != null) {
            boolean approved = request.getStatus() == RenewalStatus.APPROVED;
            String title = approved ? "Yêu cầu gia hạn được duyệt" : "Yêu cầu gia hạn bị từ chối";
            String message = approved
                    ? String.format("Hợp đồng #%d đã được gia hạn thành công đến ngày %s.",
                            contract.getContractId(), renewalRequest.getRequestedEndDate().toLocalDate())
                    : "Yêu cầu gia hạn hợp đồng #" + contract.getContractId() + " đã bị từ chối.";
            if (savedRequest.getReviewNote() != null) {
                message += " Ghi chú: " + savedRequest.getReviewNote();
            }
            notificationService.sendNotification(
                    NotificationType.RENEWAL_PROCESSED,
                    title,
                    message,
                    contract.getContractId(),
                    contract.getStudent().getUser().getId());
        }

        return renewalMapper.toResponse(savedRequest);
    }

    private Student getStudentByUserId(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    private Contract getContractForUpdate(Long contractId) {
        if (contractId == null || contractId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return contractRepository.findByContractIdForUpdate(contractId)
                .orElseThrow(() -> new AppException(ErrorCode.CONTRACT_NOT_FOUND));
    }

    private RenewalRequest getRenewalRequestForUpdate(Long renewalRequestId) {
        if (renewalRequestId == null || renewalRequestId <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return renewalRequestRepository.findByRenewalRequestIdForUpdate(renewalRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RENEWAL_REQUEST_NOT_FOUND));
    }

    private void ensureStudentOwnsContract(Contract contract, Long studentId) {
        if (contract.getStudent() == null
                || !Objects.equals(contract.getStudent().getId(), studentId)) {
            throw new AppException(ErrorCode.CONTRACT_NOT_FOUND);
        }
    }

    private void ensureStudentOwnsRequest(RenewalRequest request, Long studentId) {
        ensureStudentOwnsContract(request.getContract(), studentId);
    }

    private void ensureActiveContract(Contract contract) {
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new AppException(ErrorCode.RENEWAL_INVALID_STATUS,
                    "Chỉ có thể gia hạn hợp đồng đang ACTIVE");
        }
    }

    /** Cross-validation (plan Issue #16): không gia hạn khi còn yêu cầu trả phòng PENDING/APPROVED. */
    private void ensureNoOpenCheckoutRequest(Long contractId) {
        boolean hasOpenCheckout = checkoutRequestRepository.existsByContract_ContractIdAndStatusIn(
                contractId, List.of(CheckoutRequestStatus.PENDING, CheckoutRequestStatus.APPROVED));
        if (hasOpenCheckout) {
            throw new AppException(ErrorCode.RENEWAL_INVALID_STATUS,
                    "Hợp đồng đang có yêu cầu trả phòng chưa hoàn tất — không thể gia hạn");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }

    private void ensureRequestedEndDateIsAfterCurrent(Contract contract, LocalDateTime requestedEndDate) {
        if (contract.getEndDate() == null || requestedEndDate == null) {
            throw new AppException(ErrorCode.RENEWAL_END_DATE_INVALID);
        }
        long months = java.time.temporal.ChronoUnit.MONTHS.between(
                java.time.YearMonth.from(contract.getEndDate()), java.time.YearMonth.from(requestedEndDate));
        if (months < 1 || months > 12 || !requestedEndDate.isAfter(contract.getEndDate())
                || !requestedEndDate.equals(java.time.YearMonth.from(requestedEndDate).atEndOfMonth().atTime(23, 59, 59))) {
            throw new AppException(ErrorCode.RENEWAL_END_DATE_INVALID,
                    "Chỉ được gia hạn từ 1 đến 12 tháng, kết thúc vào ngày cuối tháng lúc 23:59:59.");
        }
    }

    private LocalDateTime resolveRequestedEnd(Contract contract, RenewalCreateRequest request) {
        LocalDateTime proposed = request.getRequestedEndDate();
        if (request.getMonths() != null) {
            if (request.getMonths() < 1 || request.getMonths() > 12 || contract.getEndDate() == null)
                throw new AppException(ErrorCode.RENEWAL_END_DATE_INVALID);
            LocalDateTime calculated = java.time.YearMonth.from(contract.getEndDate())
                    .plusMonths(request.getMonths()).atEndOfMonth().atTime(23, 59, 59);
            if (proposed != null && !proposed.equals(calculated))
                throw new AppException(ErrorCode.RENEWAL_END_DATE_INVALID);
            proposed = calculated;
        }
        ensureRequestedEndDateIsAfterCurrent(contract, proposed);
        return proposed;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void ensurePendingRequest(RenewalRequest request) {
        if (request.getStatus() != RenewalStatus.PENDING) {
            throw new AppException(ErrorCode.RENEWAL_INVALID_STATUS,
                    "Chỉ có thể xử lý yêu cầu gia hạn đang PENDING");
        }
    }
}
