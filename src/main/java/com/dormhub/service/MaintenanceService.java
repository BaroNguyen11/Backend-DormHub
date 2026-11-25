package com.dormhub.service;

import com.dormhub.dto.request.MaintenanceCreateRequest;
import com.dormhub.dto.request.MaintenanceStatusUpdateRequest;
import com.dormhub.dto.response.MaintenanceResponse;
import com.dormhub.entity.MaintenanceRequest;
import com.dormhub.entity.Contract;
import com.dormhub.entity.DormApplication;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.MaintenancePriority;
import com.dormhub.entity.enums.MaintenanceStatus;
import com.dormhub.entity.enums.MaintenanceType;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.Role;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.MaintenanceMapper;
import com.dormhub.repository.MaintenanceRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.DormApplicationRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import com.dormhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MaintenanceRepository maintenanceRepository;
    private final StudentRepository studentRepository;
    private final ContractRepository contractRepository;
    private final DormApplicationRepository dormApplicationRepository;
    private final RoomRepository roomRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final UserRepository userRepository;
    private final MaintenanceMapper maintenanceMapper;
    private final NotificationService notificationService;
    private final RoomStatusService roomStatusService;

    private Student getStudentByUserId(Long userId) {
        return studentRepository.findByUserId(userId).orElseThrow(() ->
                new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    // 1. Sinh viên tạo yêu cầu báo hỏng — suy phòng từ phân phòng hiện tại/gần nhất,
    //    KHÔNG tin roomId client gửi lên (chặn báo hỏng "hộ" phòng khác).
    @Transactional
    public MaintenanceResponse create(MaintenanceCreateRequest request, Long userId) {
        Student student = getStudentByUserId(userId);

        RoomAssignment currentAssignment = roomAssignmentRepository
                .findFirstByStudent_IdAndStatusOrderByAssignmentIdDesc(
                        student.getId(), RoomAssignmentStatus.CHECKED_IN)
                .orElse(null);
        if (currentAssignment == null) {
            throw new AppException(ErrorCode.STUDENT_NOT_IN_ANY_ROOM,
                    "Bạn hiện không đang ở phòng nào nên không thể tạo báo hỏng");
        }
        Room room = currentAssignment.getRoom();
        student.setRoom(room);
        studentRepository.save(student);
        String requestTitle = request.getAssetName() != null && !request.getAssetName().isBlank()
                ? request.getAssetName()
                : request.getTitle();

        MaintenanceRequest maintenance = MaintenanceRequest.builder()
                .title(requestTitle.trim())
                .description(request.getDescription().trim())
                .priority(request.getPriority())
                .type(request.getType())
                .imageUrl(request.getImageUrl())
                .maintenanceStatus(MaintenanceStatus.PENDING)
                .student(student)
                .room(room)
                .build();

        MaintenanceRequest saved = maintenanceRepository.save(maintenance);
        notifyManagersNewRequest(saved);
        return maintenanceMapper.toResponse(saved);
    }

    private void notifyManagersNewRequest(MaintenanceRequest maintenance) {
        List<Long> managerIds = userRepository.findAllByRole(Role.MANAGER).stream()
                .map(User::getId)
                .toList();
        if (managerIds.isEmpty()) {
            return;
        }
        notificationService.sendNotification(
                NotificationType.MAINTENANCE_STATUS_CHANGED,
                "Báo hỏng mới cần xử lý",
                String.format("Sinh viên %s (phòng %s) vừa báo hỏng: %s [%s - %s]",
                        maintenance.getStudent() != null && maintenance.getStudent().getUser() != null
                                ? maintenance.getStudent().getUser().getFullName() : "#" + maintenance.getId(),
                        maintenance.getRoom() != null ? maintenance.getRoom().getRoomNumber() : "?",
                        maintenance.getTitle(),
                        maintenance.getType(),
                        maintenance.getPriority()),
                maintenance.getId(),
                managerIds);
    }

    // 2. Sinh viên xem danh sách yêu cầu của mình (có phân trang)
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getMyRequests(Long userId, String search,
            MaintenanceStatus status, int page, int size) {
        validatePage(page, size);
        Student student = getStudentByUserId(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String query = search == null ? "" : search.trim().toLowerCase(java.util.Locale.ROOT);
        Specification<MaintenanceRequest> spec = (root, ignored, cb) -> {
            var owner = cb.equal(root.get("student").get("id"), student.getId());
            var statusMatch = status == null ? cb.conjunction()
                    : cb.equal(root.get("maintenanceStatus"), status);
            var textMatch = query.isEmpty() ? cb.conjunction()
                    : cb.or(cb.like(cb.lower(root.get("title")), "%" + query + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + query + "%"));
            return cb.and(owner, statusMatch, textMatch);
        };
        return maintenanceRepository.findAll(spec, pageable)
                .map(maintenanceMapper::toResponse);
    }

    // 3. Sinh viên xem chi tiết yêu cầu của mình
    @Transactional(readOnly = true)
    public MaintenanceResponse getMyRequest(Long maintenanceId, Long userId) {
        Student student = getStudentByUserId(userId);
        MaintenanceRequest maintenance = maintenanceRepository
                .findByIdAndStudent_Id(maintenanceId, student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));

        return maintenanceMapper.toResponse(maintenance);
    }

    // 4. Manager xem danh sách toàn bộ yêu cầu bảo trì (phân trang + lọc theo status, priority, type)
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getRequestsForManager(
            MaintenanceStatus status,
            MaintenancePriority priority,
            MaintenanceType type,
            int page,
            int size
    ) {
        validatePage(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<MaintenanceRequest> spec = (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("maintenanceStatus"), status));
            }
            if (priority != null) {
                predicates = cb.and(predicates, cb.equal(root.get("priority"), priority));
            }
            if (type != null) {
                predicates = cb.and(predicates, cb.equal(root.get("type"), type));
            }
            return predicates;
        };

        return maintenanceRepository.findAll(spec, pageable).map(maintenanceMapper::toResponse);
    }

    // 5. Manager xem chi tiết 1 yêu cầu bảo trì
    @Transactional(readOnly = true)
    public MaintenanceResponse getRequestDetailForManager(Long maintenanceId) {
        MaintenanceRequest maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));

        return maintenanceMapper.toResponse(maintenance);
    }

    // 6. Manager cập nhật trạng thái yêu cầu
    @Transactional
    public MaintenanceResponse updateStatus(Long maintenanceId, MaintenanceStatusUpdateRequest request) {
        MaintenanceRequest maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));

        MaintenanceStatus currentStatus = maintenance.getMaintenanceStatus();
        MaintenanceStatus newStatus = request.getStatus();

        if (currentStatus == newStatus) {
            return maintenanceMapper.toResponse(maintenance);
        }

        // Không cho phép chỉnh sửa yêu cầu đã kết thúc
        if (currentStatus == MaintenanceStatus.COMPLETED || currentStatus == MaintenanceStatus.REJECTED) {
            throw new AppException(ErrorCode.MAINTENANCE_INVALID_STATUS);
        }

        // PENDING -> IN_PROGRESS hoặc REJECTED
        if (currentStatus == MaintenanceStatus.PENDING
                && newStatus != MaintenanceStatus.IN_PROGRESS
                && newStatus != MaintenanceStatus.REJECTED) {
            throw new AppException(ErrorCode.MAINTENANCE_INVALID_STATUS);
        }

        // IN_PROGRESS -> COMPLETED hoặc REJECTED
        if (currentStatus == MaintenanceStatus.IN_PROGRESS
                && newStatus != MaintenanceStatus.COMPLETED
                && newStatus != MaintenanceStatus.REJECTED) {
            throw new AppException(ErrorCode.MAINTENANCE_INVALID_STATUS);
        }

        // Bắt buộc phải có lý do nếu từ chối
        if (newStatus == MaintenanceStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new AppException(ErrorCode.REJECTION_REASON_REQUIRED);
            }
            maintenance.setRejectionReason(request.getRejectionReason().trim());
        }

        maintenance.setMaintenanceStatus(newStatus);

        if (newStatus == MaintenanceStatus.COMPLETED) {
            maintenance.setResolvedAt(LocalDateTime.now(BUSINESS_ZONE));
        }

        if (newStatus == MaintenanceStatus.COMPLETED || newStatus == MaintenanceStatus.REJECTED) {
            Room room = maintenance.getRoom();
            if (room != null && room.getStatus() == RoomStatus.MAINTENANCE) {
                boolean hasOtherOpenRequests = maintenanceRepository.existsByRoom_IdAndMaintenanceStatusIn(
                        room.getId(), List.of(MaintenanceStatus.PENDING, MaintenanceStatus.IN_PROGRESS));
                if (!hasOtherOpenRequests) {
                    // Cho phép thoát MAINTENANCE và tính lại đúng cả nhánh RESERVED (hợp đồng PENDING giữ chỗ)
                    roomStatusService.refreshStatus(room, true);
                    roomRepository.save(room);
                }
            }
        }

        MaintenanceRequest updated = maintenanceRepository.save(maintenance);
        String messageBody = switch (newStatus) {
            case IN_PROGRESS -> "Yêu cầu bảo trì \"" + updated.getTitle() + "\" đã được tiếp nhận và đang tiến hành xử lý.";
            case COMPLETED -> "Yêu cầu bảo trì \"" + updated.getTitle() + "\" đã được hoàn thành.";
            case REJECTED -> "Yêu cầu bảo trì \"" + updated.getTitle() + "\" đã bị từ chối. Lý do: " + updated.getRejectionReason();
            default -> "Yêu cầu bảo trì đã đổi trạng thái sang " + newStatus.name();
        };
        notificationService.sendNotification(
                NotificationType.MAINTENANCE_STATUS_CHANGED,
                "Cập nhật yêu cầu bảo trì #" + updated.getId(),
                messageBody,
                updated.getId(),
                updated.getStudent().getUser().getId()
        );
        return maintenanceMapper.toResponse(updated);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }
}
