package com.dormhub.mapper;

import com.dormhub.dto.response.NotificationResponse;
import com.dormhub.entity.Notification;
import com.dormhub.entity.NotificationReceiver;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(NotificationReceiver receiver) {
        if (receiver == null) {
            return null;
        }

        Notification noti = receiver.getNotification();

        return NotificationResponse.builder()
                .id(receiver.getId())
                .notificationId(noti != null ? noti.getId() : null)
                .type(noti != null ? noti.getType() : null)
                .title(noti != null ? noti.getTitle() : null)
                .body(noti != null ? noti.getBody() : null)
                .refId(noti != null ? noti.getRefId() : null)
                .isRead(receiver.isRead())
                .createdAt(noti != null ? noti.getCreatedAt() : null)
                .readAt(receiver.getReadAt())
                .build();
    }
}