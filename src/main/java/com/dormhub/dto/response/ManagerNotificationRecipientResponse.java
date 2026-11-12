package com.dormhub.dto.response;

import java.time.LocalDateTime;

public record ManagerNotificationRecipientResponse(
        Long userId, String fullName, String studentCode, boolean isRead, LocalDateTime readAt) {
}
