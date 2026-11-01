package com.dormhub.service;

import com.dormhub.dto.response.NotificationResponse;
import com.dormhub.entity.Notification;
import com.dormhub.entity.NotificationReceiver;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.NotificationType;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.NotificationMapper;
import com.dormhub.repository.NotificationReceiverRepository;
import com.dormhub.repository.NotificationRepository;
import com.dormhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReceiverRepository notificationReceiverRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    // ==========================================
    // 1. SHARED API: GỬI THÔNG BÁO CHO TOÀN BỘ DỰ ÁN
    // ==========================================

    /**
     * Gửi thông báo cho nhiều người dùng cùng lúc (Ví dụ: thông báo toàn phòng, toàn tòa)
     */
    @Transactional
    public void sendNotification(NotificationType type, String title, String body, Long refId, List<Long> receiverUserIds) {
        if (receiverUserIds == null || receiverUserIds.isEmpty()) {
            return;
        }

        // 1. Tạo Notification chung
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
        log.info("Đã gửi thông báo [{}] đến {} người dùng.", type, receiverRecords.size());
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

    // ==========================================
    // 2. USER APIs: XEM & QUẢN LÝ THÔNG BÁO CỦA MÌNH
    // ==========================================

    // Lấy danh sách thông báo của user đăng nhập
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Long userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<NotificationReceiver> pageResult = unreadOnly
                ? notificationReceiverRepository.findAllByUser_IdAndIsReadFalseOrderByNotification_CreatedAtDesc(userId, pageable)
                : notificationReceiverRepository.findAllByUser_IdOrderByNotification_CreatedAtDesc(userId, pageable);

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
            receiver.setReadAt(LocalDateTime.now());
            notificationReceiverRepository.save(receiver);
        }
    }

    // Đánh dấu tất cả thông báo là đã đọc
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationReceiverRepository.markAllAsRead(userId, LocalDateTime.now());
    }
}