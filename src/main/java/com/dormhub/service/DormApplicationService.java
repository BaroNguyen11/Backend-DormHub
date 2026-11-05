package com.dormhub.service;

import com.dormhub.dto.request.DormApplicationRequest;
import com.dormhub.dto.request.DormApplicationReviewRequest;
import com.dormhub.dto.response.DormApplicationResponse;
import com.dormhub.entity.Contract;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.InvoiceType;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.DormApplicationMapper;
import com.dormhub.repository.DormApplicationRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import com.dormhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DormApplicationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<DormApplicationStatus> ACTIVE_STATUSES =
            List.of(DormApplicationStatus.PENDING, DormApplicationStatus.APPROVED);

    private final DormApplicationRepository dormApplicationRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ContractService contractService;
    private final DormApplicationMapper dormApplicationMapper;

    @Transactional
    public DormApplicationResponse submit(Long userId, DormApplicationRequest request) {
        Student student = studentRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        validateStudentProfile(student);
        if (request.getRoomId() == null) {
            throw new AppException(ErrorCode.DORM_APPLICATION_ROOM_REQUIRED);
        }
        if (dormApplicationRepository.existsByStudentIdAndStatusIn(student.getId(), ACTIVE_STATUSES)) {
            throw new AppException(ErrorCode.DORM_APPLICATION_ALREADY_ACTIVE);
        }
        Room selectedRoom = findAvailableRoom(request.getRoomId(), student);

        DormApplication application = new DormApplication();
        application.setStudent(student);
        application.setRequestedRoom(selectedRoom);
        application.setStatus(DormApplicationStatus.PENDING);
        application.setNote(normalizeNote(request.getNote()));

        return toResponse(dormApplicationRepository.save(application));
    }

    public Page<DormApplicationResponse> getMyApplications(
            Long userId,
            int page,
            int size) {
        validatePage(page, size);
        Student student = findStudentByUserId(userId);

        return toResponses(dormApplicationRepository.findAllByStudentId(student.getId(), pageable(page, size)));
    }

    public DormApplicationResponse getMyApplication(Long userId, Long applicationId) {
        Student student = findStudentByUserId(userId);
        return dormApplicationRepository.findByIdAndStudentId(applicationId, student.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.DORM_APPLICATION_NOT_FOUND));
    }

    @Transactional
    public DormApplicationResponse changeRequestedRoom(
            Long userId,
            Long applicationId,
            Long roomId) {
        Student student = studentRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        DormApplication application = dormApplicationRepository
                .findByIdAndStudentIdForUpdate(applicationId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.DORM_APPLICATION_NOT_FOUND));

        if (application.getStatus() != DormApplicationStatus.PENDING) {
            throw new AppException(ErrorCode.DORM_APPLICATION_NOT_PENDING);
        }
        application.setRequestedRoom(findAvailableRoom(roomId, student));
        return toResponse(dormApplicationRepository.save(application));
    }

    @Transactional
    public DormApplicationResponse cancel(Long userId, Long applicationId) {
        Student student = findStudentByUserId(userId);
        DormApplication application = dormApplicationRepository
                .findByIdAndStudentIdForUpdate(applicationId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.DORM_APPLICATION_NOT_FOUND));

        if (application.getStatus() != DormApplicationStatus.PENDING) {
            throw new AppException(ErrorCode.DORM_APPLICATION_NOT_PENDING);
        }

        application.setStatus(DormApplicationStatus.CANCELLED);
        return toResponse(dormApplicationRepository.save(application));
    }

    public Page<DormApplicationResponse> getAll(
            DormApplicationStatus status,
            int page,
            int size) {
        validatePage(page, size);
        Pageable pageable = pageable(page, size);
        Page<DormApplication> applications = status == null
                ? dormApplicationRepository.findAll(pageable)
                : dormApplicationRepository.findAllByStatus(status, pageable);
        return toResponses(applications);
    }

    public DormApplicationResponse getById(Long applicationId) {
        return toResponse(findApplication(applicationId));
    }

    @Transactional
    public DormApplicationResponse review(
            Long applicationId,
            DormApplicationReviewRequest request,
            Long reviewerUserId) {
        if (request.getStatus() != DormApplicationStatus.APPROVED
                && request.getStatus() != DormApplicationStatus.REJECTED) {
            throw new AppException(ErrorCode.DORM_APPLICATION_INVALID_REVIEW_STATUS);
        }
        if (request.requiresNote() && !StringUtils.hasText(request.getNote())) {
            throw new AppException(ErrorCode.DORM_APPLICATION_REJECTION_NOTE_REQUIRED);
        }

        DormApplication application = dormApplicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.DORM_APPLICATION_NOT_FOUND));
        if (application.getStatus() != DormApplicationStatus.PENDING) {
            throw new AppException(ErrorCode.DORM_APPLICATION_NOT_PENDING);
        }

        application.setStatus(request.getStatus());
        application.setReviewNote(normalizeNote(request.getNote()));
        application.setProcessedAt(LocalDateTime.now());
        if (reviewerUserId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        User reviewer = userRepository.getReferenceById(reviewerUserId);
        application.setProcessedBy(reviewer);
        if (request.getStatus() == DormApplicationStatus.APPROVED) {
            contractService.createPendingForApprovedApplication(application);
        }
        return toResponse(dormApplicationRepository.save(application));
    }

    private Page<DormApplicationResponse> toResponses(Page<DormApplication> applications) {
        List<DormApplication> content = applications.getContent();
        if (content.isEmpty()) {
            return applications.map(application -> dormApplicationMapper.toResponse(application, null, null));
        }

        List<Long> applicationIds = content.stream().map(DormApplication::getId).toList();
        Map<Long, Contract> contractsByApplicationId = contractRepository
                .findAllByDormApplication_IdIn(applicationIds).stream()
                .collect(Collectors.toMap(contract -> contract.getDormApplication().getId(), Function.identity()));
        List<Long> contractIds = contractsByApplicationId.values().stream()
                .map(Contract::getContractId).toList();
        Map<Long, Invoice> depositInvoicesByContractId = contractIds.isEmpty()
                ? Map.of()
                : invoiceRepository.findAllByContract_ContractIdInAndInvoiceType(contractIds, InvoiceType.DEPOSIT)
                        .stream().collect(Collectors.toMap(
                                invoice -> invoice.getContract().getContractId(), Function.identity()));

        return applications.map(application -> {
            Contract contract = contractsByApplicationId.get(application.getId());
            Invoice invoice = contract == null ? null : depositInvoicesByContractId.get(contract.getContractId());
            return dormApplicationMapper.toResponse(application, contract, invoice);
        });
    }

    private DormApplicationResponse toResponse(DormApplication application) {
        Contract contract = contractRepository.findByDormApplication_Id(application.getId()).orElse(null);
        Invoice invoice = contract == null ? null : invoiceRepository
                .findByContract_ContractIdAndInvoiceType(contract.getContractId(), InvoiceType.DEPOSIT)
                .orElse(null);
        return dormApplicationMapper.toResponse(application, contract, invoice);
    }

    private DormApplication findApplication(Long applicationId) {
        return dormApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.DORM_APPLICATION_NOT_FOUND));
    }

    private Student findStudentByUserId(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDate"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }

    private String normalizeNote(String note) {
        return StringUtils.hasText(note) ? note.trim() : null;
    }

    private Room findAvailableRoom(Long roomId, Student student) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (student.getGender() != Gender.MALE && student.getGender() != Gender.FEMALE) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }
        if (room.getBuilding().getGenderRestriction() == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        if (room.getBuilding().getGenderRestriction() != student.getGender()) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
        long pendingApplications = dormApplicationRepository.countByRequestedRoomIdAndStatusIn(
                room.getId(), List.of(DormApplicationStatus.PENDING));
        long pendingContracts = contractRepository.countByRoom_IdAndStatusIn(
                room.getId(), List.of(ContractStatus.PENDING));
        if (room.getCurrentOccupancy() + pendingContracts + pendingApplications >= room.getCapacity()) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }
        return room;
    }

    private void validateStudentProfile(Student student) {
        if (!StringUtils.hasText(student.getStudentCode())
                || student.getDateOfBirth() == null
                || !StringUtils.hasText(student.getAddress())) {
            throw new AppException(ErrorCode.STUDENT_PROFILE_INCOMPLETE,
                    "Hồ sơ cần có mã sinh viên, ngày sinh và địa chỉ trước khi nộp đơn");
        }
        if (student.getGender() != Gender.MALE && student.getGender() != Gender.FEMALE) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }
    }
}
