package com.dormhub.dto.response;

import com.dormhub.entity.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id; // ID của receiver (dùng để đánh dấu đã đọc)
    private Long notificationId;
    private NotificationType type;
    private String title;
    private String body;
    private Long refId;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}