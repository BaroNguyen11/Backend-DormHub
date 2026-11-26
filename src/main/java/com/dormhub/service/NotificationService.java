package com.dormhub.service;

import static com.dormhub.specification.SearchSpecifications.*;

import com.dormhub.dto.response.NotificationResponse;
import com.dormhub.dto.response.ManagerNotificationSummaryResponse;
import com.dormhub.dto.response.ManagerNotificationDetailResponse;
import com.dormhub.dto.response.ManagerNotificationRecipientResponse;
import com.dormhub.entity.Notification;
import com.dormhub.entity.NotificationReceiver;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.NotificationMapper;
import com.dormhub.repository.NotificationReceiverRepository;
import com.dormhub.repository.NotificationRepository;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    @Transactional
    public void notifyResidenceRequestManagers(String title, String body) {
        sendNotification(NotificationType.GENERAL, title, body, null,
                userRepository.findAllByRole(com.dormhub.entity.enums.Role.MANAGER).stream()
                        .map(User::getId).toList());
    }

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final NotificationRepository notificationRepository;
    private final BuildingRepository buildingRepository;
    private final NotificationReceiverRepository notificationReceiverRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public Page<ManagerNotificationSummaryResponse> getManagerHistory(
            String search, String target, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        String normalizedTarget = target == null ? "" : target.trim().toUpperCase();
        if (!List.of("", "ALL", "BUILDING", "LEGACY").contains(normalizedTarget)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        String normalizedSearch = search == null ? "" : search.trim();
        org.springframework.data.jpa.domain.Specification<com.dormhub.entity.Notification> audience = (root, query, cb) -> cb.or(
                cb.equal(root.get("type"), NotificationType.MANAGER_ANNOUNCEMENT),
                cb.and(cb.equal(root.get("type"), NotificationType.GENERAL), cb.isNull(root.get("refId"))));
        org.springframework.data.jpa.domain.Specification<com.dormhub.entity.Notification> targetFilter = (root, query, cb) -> switch (normalizedTarget) {
            case "BUILDING" -> cb.and(cb.equal(root.get("type"), NotificationType.MANAGER_ANNOUNCEMENT), cb.isNotNull(root.get("refId")));
            case "ALL" -> cb.and(cb.equal(root.get("type"), NotificationType.MANAGER_ANNOUNCEMENT), cb.isNull(root.get("refId")));
            case "LEGACY" -> cb.equal(root.get("type"), NotificationType.GENERAL);
            default -> cb.conjunction();
        };
        return notificationRepository.findAll(audience.and(targetFilter).and(text(normalizedSearch, "id", "title", "body")),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(this::toManagerSummary);
    }

    @Transactional(readOnly = true)
    public ManagerNotificationDetailResponse getManagerHistoryDetail(Long id) {
        Notification notification = notificationRepository.findById(id)
                .filter(item -> item.getType() == NotificationType.MANAGER_ANNOUNCEMENT
                        || (item.getType() == NotificationType.GENERAL && item.getRefId() == null))
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        List<ManagerNotificationRecipientResponse> recipients = notificationReceiverRepository
                .findAllByNotification_IdOrderByIdAsc(id).stream()
                .map(receiver -> new ManagerNotificationRecipientResponse(
                        receiver.getUser().getId(), receiver.getUser().getFullName(),
                        receiver.getUser().getStudent() == null ? null
                                : receiver.getUser().getStudent().getStudentCode(),
                        receiver.isRead(), receiver.getReadAt()))
                .toList();
        ManagerNotificationSummaryResponse summary = toManagerSummary(notification);
        return new ManagerNotificationDetailResponse(summary.id(), summary.title(), summary.body(),
                summary.target(), summary.buildingName(), recipients.size(), summary.createdAt(), recipients);
    }

    private ManagerNotificationSummaryResponse toManagerSummary(Notification notification) {
        String target = notification.getType() == NotificationType.GENERAL ? "LEGACY"
                : notification.getRefId() == null ? "ALL" : "BUILDING";
        String buildingName = "BUILDING".equals(target)
                ? buildingRepository.findById(notification.getRefId())
                        .map(building -> building.getName())
                        .orElse("Tòa #" + notification.getRefId())
                : null;
        return new ManagerNotificationSummaryResponse(notification.getId(), notification.getTitle(),
                notification.getBody(), target, buildingName,
                notificationReceiverRepository.countByNotification_Id(notification.getId()),
                notification.getCreatedAt());
    }

    // ==========================================
    // 1. SHARED API: GỬI THÔNG BÁO CHO TOÀN BỘ DỰ ÁN
    // ==========================================

    /**
     * Gửi thông báo cho nhiều người dùng cùng lúc (Ví dụ: thông báo toàn phòng, toàn tòa).
     * Persistence errors propagate so the surrounding transaction can roll back.
     */
    @Transactional
    public void sendNotification(NotificationType type, String title, String body, Long refId, List<Long> receiverUserIds) {
        if (receiverUserIds == null || receiverUserIds.isEmpty()) {
            return;
        }

        // 1. Tạo một Notification chung cho toàn bộ đợt gửi.
        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .body(body)
                .refId(refId)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        // 2. Tìm danh sách User nhận
        List<User> receivers = userRepository.findAllById(receiverUserIds);

        // 3. Tạo bản ghi NotificationReceiver cho từng User
        List<NotificationReceiver> receiverRecords = receivers.stream()
                .map(user -> NotificationReceiver.builder()
                        .notification(savedNotification)
                        .user(user)
                        .isRead(false)
                        .build())
                .toList();

        notificationReceiverRepository.saveAll(receiverRecords);
    }

    /**
     * Gửi thông báo cho 1 người dùng duy nhất (Overload tiện lợi)
     */
    @Transactional
    public void sendNotification(NotificationType type, String title, String body, Long refId, Long receiverUserId) {
        if (receiverUserId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "receiverUserId cannot be null");
        }
        sendNotification(type, title, body, refId, List.of(receiverUserId));
    }

    /**
     * Overload không có refId (dùng cho thông báo GENERAL từ manager)
     */
    @Transactional
    public void sendNotification(Long receiverUserId, String title, String body, NotificationType type) {
        sendNotification(type, title, body, null, receiverUserId);
    }

    // ==========================================
    // 2. USER APIs: XEM & QUẢN LÝ THÔNG BÁO CỦA MÌNH
    // ==========================================

    // Lấy danh sách thông báo của user đăng nhập
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Long userId, boolean unreadOnly,
            String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationReceiver> pageResult = notificationReceiverRepository.searchForUser(
                userId, unreadOnly, search == null ? "" : search.trim(), pageable);

        return pageResult.map(notificationMapper::toResponse);
    }

    // Đếm số lượng thông báo chưa đọc
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationReceiverRepository.countByUser_IdAndIsReadFalse(userId);
    }

    // Đánh dấu 1 thông báo là đã đọc
    @Transactional
    public void markAsRead(Long receiverId, Long userId) {
        NotificationReceiver receiver = notificationReceiverRepository.findByIdAndUser_Id(receiverId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!receiver.isRead()) {
            receiver.setRead(true);
            receiver.setReadAt(LocalDateTime.now(BUSINESS_ZONE));
            notificationReceiverRepository.save(receiver);
        }
    }

    // Đánh dấu tất cả thông báo là đã đọc
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationReceiverRepository.markAllAsRead(userId, LocalDateTime.now(BUSINESS_ZONE));
    }
}
