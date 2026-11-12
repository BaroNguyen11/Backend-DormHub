package com.dormhub.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ManagerNotificationDetailResponse(
        Long id, String title, String body, String target, String buildingName,
        long recipientCount, LocalDateTime createdAt,
        List<ManagerNotificationRecipientResponse> recipients) {
}
