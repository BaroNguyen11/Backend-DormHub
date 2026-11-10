package com.dormhub.controller;

import com.dormhub.dto.request.NotificationCreateRequest;
import com.dormhub.dto.response.ApiResponse;
import com.dormhub.dto.response.NotificationResponse;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final RoomAssignmentRepository roomAssignmentRepository;

    // 1. Lấy danh sách thông báo (hỗ trợ lọc chưa đọc: ?unreadOnly=true)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<NotificationResponse> result = notificationService.getMyNotifications(user.getId(), unreadOnly, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", result));
    }

    // 2. Đếm số lượng thông báo chưa đọc (Badge quả chuông)
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countUnread(
            @AuthenticationPrincipal User user
    ) {
        long unreadCount = notificationService.countUnread(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo chưa đọc thành công", Map.of("unreadCount", unreadCount)));
    }

    // 3. Đánh dấu 1 thông báo là đã đọc
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã đọc thành công", null));
    }

    // 4. Đánh dấu tất cả thông báo là đã đọc
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User user
    ) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu tất cả là đã đọc thành công", null));
    }

    // 5. [MANAGER] Gửi thông báo cho sinh viên
    @PostMapping("/send-to-students")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> createNotification(
            @Valid @RequestBody NotificationCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        // Get target students
        List<Student> targetStudents;
        if (request.getBuildingId() != null) {
            // Students with ASSIGNED or CHECKED_IN status in this building
            targetStudents = roomAssignmentRepository.findActiveStudentsByBuilding(
                    request.getBuildingId(),
                    List.of(RoomAssignmentStatus.ASSIGNED, RoomAssignmentStatus.CHECKED_IN)
            );
        } else {
            // All students with active room assignments
            targetStudents = roomAssignmentRepository.findAllActiveStudents(
                    List.of(RoomAssignmentStatus.ASSIGNED, RoomAssignmentStatus.CHECKED_IN)
            );
        }

        // A student may have multiple active assignment rows; keep one receiver per user.
        List<Long> uniqueUserIds = targetStudents.stream()
                .map(Student::getUser)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueUserIds.isEmpty()) {
            throw new AppException(ErrorCode.NO_STUDENTS_FOUND);
        }

        notificationService.sendNotification(
                NotificationType.GENERAL,
                request.getTitle(),
                request.getBody(),
                null,
                uniqueUserIds
        );

        return ResponseEntity.ok(ApiResponse.success(
                "Đã gửi thông báo đến " + uniqueUserIds.size() + " sinh viên",
                null
        ));
    }
}
