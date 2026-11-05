package com.dormhub.service;

import com.dormhub.dto.request.MaintenanceCreateRequest;
import com.dormhub.dto.request.MaintenanceStatusUpdateRequest;
import com.dormhub.dto.response.MaintenanceResponse;
import com.dormhub.entity.MaintenanceRequest;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.MaintenancePriority;
import com.dormhub.entity.enums.MaintenanceStatus;
import com.dormhub.entity.enums.MaintenanceType;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.MaintenanceMapper;
import com.dormhub.repository.MaintenanceRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final MaintenanceMapper maintenanceMapper;
    private final NotificationService notificationService;

    private Student getStudentByUserId(Long userId) {
        return studentRepository.findByUserId(userId).orElseThrow(() ->
                new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    // 1. Sinh viên tạo yêu cầu báo hỏng
    @Transactional
    public MaintenanceResponse create(MaintenanceCreateRequest request, Long userId) {
        Student student = getStudentByUserId(userId);

        Room room = roomRepository.findById(request.getRoomId()).orElseThrow(() ->
                new AppException(ErrorCode.ROOM_NOT_FOUND));

        MaintenanceRequest maintenance = MaintenanceRequest.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .priority(request.getPriority())
                .type(request.getType())
                .imageUrl(request.getImageUrl())
                .maintenanceStatus(MaintenanceStatus.PENDING)
                .student(student)
                .room(room)
                .build();

        MaintenanceRequest saved = maintenanceRepository.save(maintenance);
        return maintenanceMapper.toResponse(saved);
    }

    // 2. Sinh viên xem danh sách yêu cầu của mình (có phân trang)
    @Transactional(readOnly = true)
    public Page<MaintenanceResponse> getMyRequests(Long userId, int page, int size) {
        Student student = getStudentByUserId(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return maintenanceRepository.findAllByStudent_Id(student.getId(), pageable)
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
            maintenance.setResolvedAt(LocalDateTime.now());
        }

        if (newStatus == MaintenanceStatus.COMPLETED || newStatus == MaintenanceStatus.REJECTED) {
            Room room = maintenance.getRoom();
            if (room != null && room.getStatus() == RoomStatus.MAINTENANCE) {
                boolean hasOtherOpenRequests = maintenanceRepository.existsByRoom_IdAndMaintenanceStatusIn(
                        room.getId(), List.of(MaintenanceStatus.PENDING, MaintenanceStatus.IN_PROGRESS));
                if (!hasOtherOpenRequests) {
                    room.setStatus(room.getCurrentOccupancy() >= room.getCapacity()
                            ? RoomStatus.OCCUPIED
                            : RoomStatus.AVAILABLE);
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
}