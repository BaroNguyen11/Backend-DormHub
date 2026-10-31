package com.dormhub.service;

import com.dormhub.dto.request.DormApplicationRequest;
import com.dormhub.dto.request.DormApplicationReviewRequest;
import com.dormhub.dto.response.DormApplicationResponse;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.BuildingGenderPolicy;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.DormApplicationMapper;
import com.dormhub.repository.DormApplicationRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DormApplicationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<DormApplicationStatus> ACTIVE_STATUSES =
            List.of(DormApplicationStatus.PENDING, DormApplicationStatus.APPROVED);

    private final DormApplicationRepository dormApplicationRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RoomAssignmentService roomAssignmentService;
    private final DormApplicationMapper dormApplicationMapper;

    @Transactional
    public DormApplicationResponse submit(Long userId, DormApplicationRequest request) {
        Student student = studentRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        validateStudentProfile(student);
        Room selectedRoom;
        if (student.getGender() == Gender.OTHER) {
            if (request.getRoomId() != null) {
                throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
            }
            selectedRoom = null;
        } else {
            if (request.getRoomId() == null) {
                throw new AppException(ErrorCode.DORM_APPLICATION_ROOM_REQUIRED);
            }
            selectedRoom = findAvailableRoom(request.getRoomId(), student);
        }

        if (dormApplicationRepository.existsByStudentIdAndStatusIn(student.getId(), ACTIVE_STATUSES)) {
            throw new AppException(ErrorCode.DORM_APPLICATION_ALREADY_ACTIVE);
        }

        DormApplication application = new DormApplication();
        application.setStudent(student);
        application.setRequestedRoom(selectedRoom);
        application.setStatus(DormApplicationStatus.PENDING);
        application.setNote(normalizeNote(request.getNote()));

        return dormApplicationMapper.toResponse(dormApplicationRepository.save(application));
    }

    public Page<DormApplicationResponse> getMyApplications(
            Long userId,
            int page,
            int size) {
        validatePage(page, size);
        Student student = findStudentByUserId(userId);

        return dormApplicationRepository.findAllByStudentId(student.getId(), pageable(page, size))
                .map(dormApplicationMapper::toResponse);
    }

    public DormApplicationResponse getMyApplication(Long userId, Long applicationId) {
        Student student = findStudentByUserId(userId);
        return dormApplicationRepository.findByIdAndStudentId(applicationId, student.getId())
                .map(dormApplicationMapper::toResponse)
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
        if (student.getGender() == Gender.OTHER) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }

        application.setRequestedRoom(findAvailableRoom(roomId, student));
        return dormApplicationMapper.toResponse(dormApplicationRepository.save(application));
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
        return dormApplicationMapper.toResponse(dormApplicationRepository.save(application));
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
        return applications.map(dormApplicationMapper::toResponse);
    }

    public DormApplicationResponse getById(Long applicationId) {
        return dormApplicationMapper.toResponse(findApplication(applicationId));
    }

    @Transactional
    public DormApplicationResponse review(
            Long applicationId,
            DormApplicationReviewRequest request) {
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
        if (request.getStatus() == DormApplicationStatus.APPROVED) {
            roomAssignmentService.assign(applicationId);
        }
        return dormApplicationMapper.toResponse(dormApplicationRepository.save(application));
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
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }
        if (room.getBuilding().getGenderPolicy() == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        BuildingGenderPolicy policy = room.getBuilding().getGenderPolicy();
        if (student.getGender() == Gender.MALE && policy != BuildingGenderPolicy.MALE_ONLY
                || student.getGender() == Gender.FEMALE && policy != BuildingGenderPolicy.FEMALE_ONLY) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
        long occupiedSlots = roomAssignmentRepository.countByRoomIdAndStatus(
                room.getId(), RoomAssignmentStatus.ASSIGNED);
        if (occupiedSlots >= room.getCapacity()) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }
        return room;
    }

    private void validateStudentProfile(Student student) {
        if (!StringUtils.hasText(student.getStudentCode())
                || student.getDateOfBirth() == null
                || student.getGender() == null
                || !StringUtils.hasText(student.getAddress())) {
            throw new AppException(ErrorCode.STUDENT_PROFILE_INCOMPLETE,
                    "Hồ sơ cần có mã sinh viên, ngày sinh, giới tính và địa chỉ trước khi nộp đơn");
        }
    }
}
