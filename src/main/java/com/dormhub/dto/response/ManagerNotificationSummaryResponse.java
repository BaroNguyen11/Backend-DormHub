package com.dormhub.dto.response;

import java.time.LocalDateTime;

public record ManagerNotificationSummaryResponse(
        Long id, String title, String body, String target, String buildingName,
        long recipientCount, LocalDateTime createdAt) {
}
