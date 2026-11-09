package com.dormhub.dto.response;

import com.dormhub.entity.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isRead") // Lombok generates isRead() method → JSON key would be "read", force it to "isRead"
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}