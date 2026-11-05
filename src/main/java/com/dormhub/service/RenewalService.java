package com.dormhub.service;

import com.dormhub.dto.request.RenewalCreateRequest;
import com.dormhub.dto.request.RenewalReviewRequest;
import com.dormhub.dto.response.RenewalResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.RenewalRequest;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RenewalStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RenewalMapper;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.RenewalRequestRepository;
import com.dormhub.repository.StudentRepository;
import com.dormhub.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenewalService {

    private final ContractRepository contractRepository;
    private final RenewalRequestRepository renewalRequestRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RenewalMapper renewalMapper;
    private final NotificationService notificationService;

    public RenewalService(ContractRepository contractRepository,
                          RenewalRequestRepository renewalRequestRepository,
                          StudentRepository studentRepository,
                          UserRepository userRepository,
                          RenewalMapper renewalMapper,
                          NotificationService notificationService) {
        this.contractRepository = contractRepository;
        this.renewalRequestRepository = renewalRequestRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.renewalMapper = renewalMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public RenewalResponse create(Long contractId, RenewalCreateRequest request, Long userId) {
        if (request == null || request.getRequestedEndDate() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Student student = getStudentByUserId(userId);
        Contract contract = getContractForUpdate(contractId);
        ensureStudentOwnsContract(contract, student.getId());
        ensureActiveContract(contract);
        ensureRequestedEndDateIsAfterCurrent(contract, request.getRequestedEndDate());

        if (renewalRequestRepository.existsByContract_ContractIdAndStatus(
                contractId, RenewalStatus.PENDING)) {
            throw new AppException(ErrorCode.RENEWAL_ALREADY_PENDING);
        }

        RenewalRequest renewalRequest = new RenewalRequest();
        renewalRequest.setContract(contract);
        renewalRequest.setRequestedEndDate(request.getRequestedEndDate());
        renewalRequest.setReason(trimToNull(request.getReason()));
        renewalRequest.setStatus(RenewalStatus.PENDING);
        renewalRequest.setRequestedAt(LocalDateTime.now());

        return renewalMapper.toResponse(renewalRequestRepository.save(renewalRequest));
    }

    @Transactional(readOnly = true)
    public Page<RenewalResponse> getMyRequests(Long userId, int page, int size) {
        Student student = getStudentByUserId(userId);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        return renewalRequestRepository.findAllByContract_Student_Id(student.getId(), pageable)
                .map(renewalMapper::toResponse);
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
            RenewalStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<RenewalRequest> requests = status == null
                ? renewalRequestRepository.findAll(pageable)
                : renewalRequestRepository.findAllByStatus(status, pageable);
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
            ensureActiveContract(contract);
            ensureRequestedEndDateIsAfterCurrent(contract, renewalRequest.getRequestedEndDate());
            contract.setEndDate(renewalRequest.getRequestedEndDate());
            contractRepository.save(contract);
        }

        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        renewalRequest.setStatus(request.getStatus());
        renewalRequest.setReviewedBy(reviewer);
        renewalRequest.setReviewedAt(LocalDateTime.now());
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
                    NotificationType.GENERAL,
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

    private void ensureRequestedEndDateIsAfterCurrent(
            Contract contract, LocalDateTime requestedEndDate) {
        if (contract.getEndDate() == null || !requestedEndDate.isAfter(contract.getEndDate())) {
            throw new AppException(ErrorCode.RENEWAL_END_DATE_INVALID);
        }
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
